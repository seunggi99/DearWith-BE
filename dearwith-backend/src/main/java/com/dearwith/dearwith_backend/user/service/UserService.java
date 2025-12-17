package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.user.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.Agreement;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.entity.UserWithdrawal;
import com.dearwith.dearwith_backend.user.enums.AgreementType;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.repository.SocialAccountRepository;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.user.repository.UserWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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
    private final UserReader userReader;
    private final StringRedisTemplate stringRedisTemplate;
    private final BusinessLogService businessLogService;
    private final UserWithdrawalRepository userWithdrawalRepository;

    private String passwordVerifiedKey(UUID userId) {
        return "user:password:verified:" + userId;
    }

    private static final Duration PASSWORD_VERIFY_TTL = Duration.ofMinutes(5);
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

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_SIGN_UP,
                user.getId(),
                TargetType.USER,
                user.getId().toString(),
                "일반 회원가입 성공",
                Map.of(
                        "email", user.getEmail(),
                        "nickname", user.getNickname()
                )
        );

        return SignUpResponseDto.builder()
                .message("회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }

    /*──────────────────────────────────────────────
    | 1-1. 관리자 전용 회원 생성 (이메일 인증 없음, 역할 추가)
     *──────────────────────────────────────────────*/
    public SignUpResponseDto signUpByAdmin(AdminCreateUserRequestDto request) {

        validateDuplicateUserByEmail(request.getEmail());
        validateDuplicateUserByNickname(request.getNickname());
        validateRequiredAgreements(request.getAgreements());

        boolean pushAgreed = isPushAgreed(request.getAgreements());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(request.getRole())
                .userStatus(UserStatus.ACTIVE)
                .lastLoginAt(null)
                .eventNotificationEnabled(pushAgreed)
                .serviceNotificationEnabled(pushAgreed)
                .build();

        addAgreements(user, request.getAgreements());

        save(user);

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_SIGN_UP_BY_ADMIN,
                null,
                TargetType.USER,
                user.getId().toString(),
                "관리자 생성 회원가입 성공",
                Map.of(
                        "email", user.getEmail(),
                        "nickname", user.getNickname(),
                        "role", user.getRole().name()
                )
        );


        return SignUpResponseDto.builder()
                .message("관리자 생성 회원가입 성공")
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

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_SOCIAL_SIGN_UP,
                user.getId(),
                TargetType.USER,
                user.getId().toString(),
                providerKor + " 회원가입 성공",
                Map.of(
                        "provider", request.getProvider().name(),
                        "socialId", request.getSocialId(),
                        "nickname", user.getNickname()
                )
        );


        return SignUpResponseDto.builder()
                .message(providerKor + " 회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }

    /*──────────────────────────────────────────────
     | 3. 조회/저장 관련 유틸
     *──────────────────────────────────────────────*/

    public User findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.INVALID_EMAIL,
                        ErrorCode.INVALID_EMAIL.getMessage()
                ));
    }

    public List<User> findAll() {
        return userRepository.findAll();
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
    public void resetPassword(PasswordResetRequestDto request) {

        // 1) 이메일 인증 티켓 검증
        emailVerifyTicketService.confirmForPurposeAndEmail(
                request.getEmailTicket(),
                EmailVerificationPurpose.RESET_PASSWORD,
                request.getEmail()
        );

        // 2) 유저 조회
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> BusinessException.withMessage(ErrorCode.NOT_FOUND, "가입되지 않은 이메일입니다."));

        // 3) 비밀번호 변경
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_PASSWORD_RESET,
                user.getId(),
                TargetType.USER,
                user.getId().toString(),
                "비밀번호 재설정 성공",
                Map.of(
                        "via", "EMAIL_TICKET"
                )
        );
    }

    @Transactional
    public void  changePassword(PasswordChangeRequestDto request, UUID userId) {
        User user = userReader.getUser(userId);

        String key = passwordVerifiedKey(userId);

        String verified = stringRedisTemplate.opsForValue().get(key);
        if (verified == null) {
            throw BusinessException.withMessage(
                    ErrorCode.UNAUTHORIZED,
                    "비밀번호 확인이 만료되었습니다. 다시 확인 후 변경해주세요."
            );
        }

        String rawNewPassword = request.getNewPassword();

        if (passwordEncoder.matches(rawNewPassword, user.getPassword())) {
            throw BusinessException.withMessage(
                    ErrorCode.INVALID_INPUT,
                    "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."
            );
        }

        user.changePassword(passwordEncoder.encode(rawNewPassword));

        stringRedisTemplate.delete(key);

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_PASSWORD_CHANGE,
                userId,
                TargetType.USER,
                userId.toString(),
                "비밀번호 변경 성공",
                Map.of()
        );
    }

    @Transactional(readOnly = true)
    public void verifyCurrentPassword(PasswordVerifyRequestDto request, UUID userId) {
        User user = userReader.getUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.withMessage(
                    ErrorCode.INVALID_INPUT,
                    "비밀번호가 올바르지 않습니다."
            );
        }

        String key = passwordVerifiedKey(userId);
        stringRedisTemplate
                .opsForValue()
                .set(key, "OK", PASSWORD_VERIFY_TTL);
    }

    @Transactional
    public void updateLastLoginAt(User user) {
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    @Transactional
    public void updateNickname(UUID userId, String newNickname) {
        User user = userReader.getLoginAllowedUser(userId);
        String oldNickname = user.getNickname();

        validateDuplicateUserByNickname(newNickname);
        user.updateNickname(newNickname);
        userRepository.save(user);

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_NICKNAME_CHANGE,
                userId,
                TargetType.USER,
                userId.toString(),
                "닉네임 변경 성공",
                Map.of(
                        "oldNickname", oldNickname,
                        "newNickname", newNickname
                )
        );
    }

    @Transactional
    public void withdraw(UUID userId, UserWithdrawRequestDto req) {
        req.validate();

        User user = userReader.getUser(userId);

        if (user.isDeleted()) {
            return;
        }

        if (!userWithdrawalRepository.existsByUser_Id(userId)) {
            userWithdrawalRepository.save(
                    UserWithdrawal.of(user, req.reason(), req.detail())
            );
        }

        user.softDelete();
        userRepository.save(user);

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_DELETE,
                userId,
                TargetType.USER,
                userId.toString(),
                "회원 탈퇴(soft delete) 처리",
                Map.of(
                        "withdrawalReason", req.reason().name(),
                        "withdrawalDetail", req.detail() == null ? "" : req.detail()
                )
        );
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

    @Transactional
    public void suspendUserByAdmin(UUID userId, UUID adminId, AdminSuspendUserRequestDto request) {
        User user = userReader.getUser(userId);

        if (user.isAdmin()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ACCESS_DENIED,
                    "관리자 계정은 정지할 수 없습니다.",
                    "CANNOT_SUSPEND_ADMIN:" + userId
            );
        }

        user.suspend(request.reason(), request.until());

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_SUSPEND,
                adminId,
                TargetType.USER,
                userId.toString(),
                "관리자에 의한 회원 정지",
                Map.of(
                        "reason", request.reason(),
                        "until", String.valueOf(request.until())
                )
        );
    }

    @Transactional
    public void writeRestrictUserByAdmin(UUID userId, UUID adminId, AdminSuspendUserRequestDto request) {
        User user = userReader.getUser(userId);

        if (user.isAdmin()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ACCESS_DENIED,
                    "관리자 계정에는 작성 제한을 걸 수 없습니다.",
                    "CANNOT_WRITE_RESTRICT_ADMIN:" + userId
            );
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (user.isSuspendedNow(today)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미 정지된 회원입니다. 정지 해제 후 작성 제한을 적용하세요.",
                    "CANNOT_WRITE_RESTRICT_SUSPENDED_USER:" + userId
            );
        }

        user.restrictWrite(request.reason(), request.until());

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_WRITE_RESTRICT,
                adminId,
                TargetType.USER,
                userId.toString(),
                "관리자에 의한 회원 작성 제한",
                Map.of(
                        "reason", request.reason(),
                        "until", String.valueOf(request.until())
                )
        );
    }

    @Transactional
    public void unsuspendUserByAdmin(UUID userId, UUID adminId) {
        User user = userReader.getUser(userId);

        user.unsuspend();

        businessLogService.info(
                BusinessLogCategory.USER,
                BusinessAction.User.USER_UNSUSPEND,
                adminId,
                TargetType.USER,
                userId.toString(),
                "관리자에 의한 회원 제재 해제",
                Map.of()
        );
    }

    /*────────────────────────────
     | 프로필 이미지 수정
     *────────────────────────────*/
    @Transactional
    public void updateProfileImage(UUID userId, ProfileImageUpdateRequestDto dto) {
        User user = userReader.getLoginAllowedUser(userId);
        userImageAppService.update(user, dto.getTmpKey());
    }

    /*────────────────────────────
     | 프로필 이미지 삭제
     *────────────────────────────*/
    @Transactional
    public void deleteProfileImage(UUID userId) {
        User user = userReader.getLoginAllowedUser(userId);
        userImageAppService.delete(user);
    }

    /*──────────────────────────────────────────────
     | 5. 조회용 DTO 변환
     *──────────────────────────────────────────────*/

    // 회원 본인 정보 조회
    public UserResponseDto getCurrentUser(UUID userId) {
        User user = userReader.getUser(userId);
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
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw BusinessException.of(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    public void validateDuplicateUserByNickname(String nickname) {
        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
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