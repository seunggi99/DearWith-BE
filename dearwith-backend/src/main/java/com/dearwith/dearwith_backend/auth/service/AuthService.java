package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.user.domain.Agreement;
import com.dearwith.dearwith_backend.user.domain.enums.AgreementType;
import com.dearwith.dearwith_backend.user.domain.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.domain.enums.Role;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.domain.enums.UserStatus;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public SignUpResponseDto signUp(SignUpRequestDto request) {

        validateRequiredAgreements(request.getAgreements());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .provider(AuthProvider.LOCAL.name())
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastLoginAt(null)
                .build();

        List<Agreement> agreementEntities = request.getAgreements().stream()
                .map(dto -> Agreement.builder()
                        .type(AgreementType.from(dto.getType()))
                        .agreed(dto.isAgreed())
                        .agreedAt(LocalDateTime.now())
                        .user(user)
                        .build()
                ).collect(Collectors.toList());

        user.getAgreements().addAll(agreementEntities);
        validateDuplicateUserByEmail(user.getEmail());
        validateDuplicateUserByNickname(user.getNickname());

        userRepo.save(user);

        return SignUpResponseDto.builder()
                .message("회원가입 성공")
                .nickname(user.getNickname())
                .build();
    }

    public SignInResponseDto signIn(SignInRequestDto request){
        // 1. 인증 처리 (예외는 전역 핸들러에서 처리)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. 인증 성공 → DB에서 User 조회
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_EMAIL));

        user.updateLastLoginAt();
        userRepo.save(user);

        // 3. 토큰 발급 등 후처리
        TokenCreateRequestDTO tokenDTO = TokenCreateRequestDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        String token = jwtTokenProvider.generateToken(tokenDTO);
        String refreshToken = jwtTokenProvider.generateRefreshToken(tokenDTO);

        // 4. 응답 DTO 리턴
        return SignInResponseDto.builder()
                .message("로그인 성공")
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenReissueResponseDTO reissueToken(JwtTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getToken();

        // 1. refreshToken에서 userId(PK) 추출
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 2. 토큰 유효성 검증 (토큰 주인과 일치 여부까지)
        if (!jwtTokenProvider.validateToken(refreshToken, user.getId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 새 AccessToken 발급
        TokenCreateRequestDTO tokenDTO = TokenCreateRequestDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
        String newAccessToken = jwtTokenProvider.generateToken(tokenDTO);

        // 4. 응답 DTO 리턴
        return TokenReissueResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .expirationTime("10min")
                .message("재발급 성공")
                .build();
    }


    public void validateToken(JwtTokenDto tokenDto) {
        String token = tokenDto.getToken();

        // 1. 토큰에서 userId(PK) 추출
        UUID userId = jwtTokenProvider.extractUserId(token);

        // 2. DB에서 유저 조회
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 3. 토큰 유효성 검사 (PK 기준)
        if (!jwtTokenProvider.validateToken(token, userId)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 아무런 예외가 없으면 OK(200) 응답
    }
    public void validateDuplicateUserByEmail(String email) {
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public void validateDuplicateUserByNickname(String nickname) {
        if (userRepo.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    public void validateRequiredAgreements(List<AgreementDto> agreementDto) {
        Set<AgreementType> agreedTypes = agreementDto.stream()
                .filter(AgreementDto::isAgreed)
                .map(dto -> AgreementType.from(dto.getType()))
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
