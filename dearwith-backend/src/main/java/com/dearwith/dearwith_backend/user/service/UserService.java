package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.user.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.Agreement;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AgreementType;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.repository.SocialAccountRepository;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocialAccountService socialAccountService;
    private final SocialAccountRepository socialAccountRepository;
    private final EmailVerifyTicketService emailVerifyTicketService;
    private final UserImageAppService userImageAppService;

    /*──────────────────────────────────────────────
     | 1. 일반 회원가입
     *──────────────────────────────────────────────*/
    public SignUpResponseDto signUp(SignUpRequestDto request) {

        // 1) 약관 / 중복 검사
        validateRequiredAgreements(request.getAgreements());
        validateDuplicateUserByEmail(request.getEmail());
        validateDuplicateUserByNickname(request.getNickname());

        // 2) 이메일 인증 티켓 검증
        emailVerifyTicketService.confirmForPurposeAndEmail(
                request.getEmailTicket(),
                EmailVerificationPurpose.SIGNUP,
                request.getEmail()
        );

        // 3) User 생성
        boolean pushAgreed = isPushAgreed(request.getAgreements());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .lastLoginAt(null)
                .eventNotificationEnabled(pushAgreed)
                .serviceNotificationEnabled(pushAgreed)
                .build();

        addAgreements(user, request.getAgreements());
        save(user);

        return SignUpResponseDto.builder()
                .message("회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }

    /*──────────────────────────────────────────────
     | 2. 카카오 회원가입
     *──────────────────────────────────────────────*/
    @Transactional
    public SignUpResponseDto socialSignUp(SocialSignUpRequestDto request) {

        // 0) 지원하는 provider 인지 체크
        if (request.getProvider() == null ||
                (request.getProvider() != AuthProvider.KAKAO
                        && request.getProvider() != AuthProvider.APPLE)) {
            throw BusinessException.withMessage(
                    ErrorCode.INVALID_SOCIAL_PROVIDER,
                    "지원하지 않는 소셜 로그인 제공자입니다."
            );
        }

        // 1) 약관 체크
        validateRequiredAgreements(request.getAgreements());

        // 2) 닉네임 중복 체크
        validateDuplicateUserByNickname(request.getNickname());

        // 3) 이미 소셜 계정이 존재하는지 한 번 더 체크
        if (socialAccountRepository.existsByProviderAndSocialId(
                request.getProvider(), request.getSocialId())) {
            throw BusinessException.withMessage(
                    ErrorCode.DUPLICATE_SOCIAL_ACCOUNT,
                    "이미 연결된 소셜 계정입니다."
            );
        }

        // 3) User 생성 (이메일/패스워드는 null)
        boolean pushAgreed = isPushAgreed(request.getAgreements());

        User user = User.builder()
                .email(null)
                .password(null)
                .nickname(request.getNickname())
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .lastLoginAt(null)
                .eventNotificationEnabled(pushAgreed)
                .serviceNotificationEnabled(pushAgreed)
                .build();

        save(user);

        // 4) 약관 저장
        addAgreements(user, request.getAgreements());

        // 5) 소셜 계정 연결 (provider/socialId 유효성은 내부에서 한 번 더 검증)
        socialAccountService.connectSocialAccount(
                user,
                request.getProvider(),
                request.getSocialId()
        );

        String providerKor = switch (request.getProvider()) {
            case KAKAO -> "카카오";
            case APPLE -> "애플";
            default -> "소셜";
        };


        return SignUpResponseDto.builder()
                .message(providerKor + " 회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }

    /*──────────────────────────────────────────────
     | 3. 조회/저장 관련 유틸
     *──────────────────────────────────────────────*/

    // 로그인 등에서 사용: "가입되지 않은 이메일입니다." 케이스
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.INVALID_EMAIL,
                        ErrorCode.INVALID_EMAIL.getMessage()    // "가입되지 않은 이메일입니다."
                ));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
    }

    private boolean isPushAgreed(List<AgreementDto> agreements) {
        if (agreements == null) return false;

        return agreements.stream()
                .anyMatch(a -> a.getType() == AgreementType.PUSH_NOTIFICATION && a.isAgreed());
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /*──────────────────────────────────────────────
     | 4. 정보 수정 / 삭제
     *──────────────────────────────────────────────*/


    @Transactional
    public void changePassword(PasswordChangeRequestDto request) {

        // 1) 이메일 인증 티켓 검증
        emailVerifyTicketService.confirmForPurposeAndEmail(
                request.getEmailTicket(),
                EmailVerificationPurpose.RESET_PASSWORD,
                request.getEmail()
        );

        // 2) 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.withMessage(ErrorCode.NOT_FOUND, "가입되지 않은 이메일입니다."));

        // 3) 비밀번호 변경
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void updateLastLoginAt(User user) {
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    @Transactional
    public void updateNickname(UUID userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));

        validateDuplicateUserByNickname(newNickname);
        user.updateNickname(newNickname);
        userRepository.save(user);
    }

    @Transactional
    public void deleteById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));

        user.markDeleted();
        userRepository.save(user);
    }

    @Transactional
    public void addAgreements(User user, List<AgreementDto> agreementDto) {
        if (agreementDto == null || agreementDto.isEmpty()) {
            return;
        }

        List<Agreement> agreements = agreementDto.stream()
                .map(dto -> Agreement.builder()
                        .type(dto.getType())
                        .agreed(dto.isAgreed())
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        user.getAgreements().addAll(agreements);
    }

    /*────────────────────────────
     | 프로필 이미지 수정
     *────────────────────────────*/
    @Transactional
    public void updateProfileImage(UUID userId, ProfileImageUpdateRequestDto dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        userImageAppService.update(user, dto.getTmpKey());
    }

    /*────────────────────────────
     | 프로필 이미지 삭제
     *────────────────────────────*/
    @Transactional
    public void deleteProfileImage(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        userImageAppService.delete(user);
    }

    /*──────────────────────────────────────────────
     | 5. 조회용 DTO 변환
     *──────────────────────────────────────────────*/

    // 회원 본인 정보 조회
    public UserResponseDto getCurrentUser(UUID userId) {
        User user = findOne(userId);
        return new UserResponseDto(user);
    }

    // 모든 회원 조회
    public List<UserResponseDto> getAllUsers() {
        return findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    /*──────────────────────────────────────────────
     | 6. 검증 유틸
     *──────────────────────────────────────────────*/

    public void validateDuplicateUserByEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.of(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void validateDuplicateUserByNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw BusinessException.of(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    public void validateRequiredAgreements(List<AgreementDto> agreementDto) {
        if (agreementDto == null || agreementDto.isEmpty()) {
            throw BusinessException.of(ErrorCode.REQUIRED_AGREEMENT_NOT_CHECKED);
        }

        Set<AgreementType> agreedTypes = agreementDto.stream()
                .filter(AgreementDto::isAgreed)
                .map(AgreementDto::getType)
                .collect(Collectors.toSet());

        List<AgreementType> requiredTypes = Arrays.stream(AgreementType.values())
                .filter(AgreementType::isRequired)
                .toList();

        boolean allRequired = requiredTypes.stream().allMatch(agreedTypes::contains);

        if (!allRequired) {
            throw BusinessException.of(ErrorCode.REQUIRED_AGREEMENT_NOT_CHECKED);
        }
    }
}