package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import com.dearwith.dearwith_backend.auth.dto.SignUpRequestDto;
import com.dearwith.dearwith_backend.auth.dto.SignUpResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.Agreement;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AgreementType;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.UserResponseDto;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private final SocialAccountService socialAccountService;

    public SignUpResponseDto signUp(SignUpRequestDto request) {

        validateRequiredAgreements(request.getAgreements());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .lastLoginAt(null)
                .build();

        addAgreements(user, request.getAgreements());
        validateDuplicateUserByEmail(user.getEmail());
        validateDuplicateUserByNickname(user.getNickname());
        save(user);
        socialAccountService.connectSocialAccount(user, AuthProvider.LOCAL, request.getEmail());

        return SignUpResponseDto.builder()
                .message("회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }


    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLoginAt(User user) {
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    @Transactional
    public void updateNickname(UUID userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateDuplicateUserByNickname(newNickname);
        user.updateNickname(newNickname);
        userRepository.save(user);
    }

    @Transactional
    public void deleteById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.markDeleted();
        userRepository.save(user);
    }
    @Transactional
    public void addAgreements(User user, List<AgreementDto> agreementDto) {
        List<Agreement> agreements = agreementDto.stream()
                .map(dto -> Agreement.builder()
                        .type(dto.getType())
                        .agreed(dto.isAgreed())
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        user.getAgreements().addAll(agreements);
    }

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

    public void validateDuplicateUserByEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public void validateDuplicateUserByNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    public void validateRequiredAgreements(List<AgreementDto> agreementDto) {
        Set<AgreementType> agreedTypes = agreementDto.stream()
                .filter(AgreementDto::isAgreed)
                .map(dto -> dto.getType())
                .collect(Collectors.toSet());

        List<AgreementType> requiredTypes = Arrays.stream(AgreementType.values())
                .filter(AgreementType::isRequired)
                .toList();

        boolean allRequired = requiredTypes.stream().allMatch(agreedTypes::contains);

        if (!allRequired) {
            throw new BusinessException(ErrorCode.REQUIRED_AGREEMENT_NOT_CHECKED);
        }
    }
}
