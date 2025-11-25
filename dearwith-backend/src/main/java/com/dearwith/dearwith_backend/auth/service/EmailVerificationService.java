package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.dto.EmailRequestDto;
import com.dearwith.dearwith_backend.auth.dto.EmailTicketResponseDto;
import com.dearwith.dearwith_backend.auth.dto.EmailVerifyRequestDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.dto.EmailVerificationPurpose;
import com.dearwith.dearwith_backend.user.service.EmailVerifyTicketService;
import com.dearwith.dearwith_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redis;
    private final MailSender mailSender;
    private final EmailVerifyTicketService emailVerifyTicketService;
    private final UserService userService;

    /*──────────────────────────────────────────────
     | 1. 인증 코드 발송
     *──────────────────────────────────────────────*/
    public void sendVerificationCode(EmailRequestDto request) {

        switch (request.getPurpose()) {
            case SIGNUP -> {
                // 가입: 아직 없는 이메일이어야 함
                userService.validateDuplicateUserByEmail(request.getEmail()); // 중복이면 예외
            }
            case RESET_PASSWORD -> {
                // 비번 변경: 반드시 가입된 이메일이어야 함
                if (!userService.existsByEmail(request.getEmail())) {
                    throw BusinessException.withMessage(ErrorCode.INVALID_EMAIL, "가입되지 않은 이메일입니다.");
                }
            }
        }

        String keyPrefix = switch (request.getPurpose()) {
            case SIGNUP -> "signup:verify:";
            case RESET_PASSWORD -> "password-reset:verify:";
        };

        String code = UUID.randomUUID().toString().substring(0, 6);
        String key = keyPrefix + request.getEmail();

        // 1) Redis 저장 (TTL 10분)
        try {
            redis.opsForValue().set(key, code, Duration.ofMinutes(10));
        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.OPERATION_FAILED,
                    "인증 메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    "REDIS_SAVE_FAILED",
                    "Redis save failed for key=" + key + ", error=" + e.getMessage(),
                    e
            );
        }

        // 2) 메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getEmail());
            message.setSubject("[DearWith] 인증 코드");
            message.setText(String.format(
                    "안녕하세요, DearWith입니다.\n\n" +
                            "이메일 인증 코드는 [%s] 입니다.\n" +
                            "인증 코드는 10분간 유효합니다.\n\n" +
                            "본 메일은 발신 전용입니다.",
                    code
            ));
            mailSender.send(message);

        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.OPERATION_FAILED,
                    "인증 메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    "EMAIL_SEND_FAILED",
                    "Mail send failed for email=" + request.getEmail() + ", error=" + e.getMessage(),
                    e
            );
        }
    }


    /*──────────────────────────────────────────────
     | 2. 인증 코드 검증 → 이메일 티켓 발급
     *──────────────────────────────────────────────*/
    public EmailTicketResponseDto verifyCode(EmailVerifyRequestDto request) {
        String email = request.getEmail();
        String code = request.getCode();
        EmailVerificationPurpose purpose = request.getPurpose();

        // 0) Redis 키 prefix 를 목적에 따라 분리
        String keyPrefix = switch (purpose) {
            case SIGNUP -> "signup:verify:";
            case RESET_PASSWORD -> "password-reset:verify:";
            default -> "verify:";
        };
        String key = keyPrefix + email;
        String saved;

        // 1) Redis 조회
        try {
            saved = redis.opsForValue().get(key);
        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.OPERATION_FAILED,
                    "인증 코드 확인 중 오류가 발생했습니다. 다시 시도해주세요.",
                    "REDIS_READ_FAILED",
                    "Redis read failed for key=" + key + ", error=" + e.getMessage(),
                    e
            );
        }

        // 2) 코드 불일치 또는 만료
        if (saved == null || !saved.equals(code)) {
            throw BusinessException.withMessage(
                    ErrorCode.INVALID_VERIFICATION_CODE,
                    "유효하지 않은 인증 코드입니다."
            );
        }

        // 3) 인증 성공 → Redis 삭제
        try {
            redis.delete(key);
        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.OPERATION_FAILED,
                    "인증 처리 중 오류가 발생했습니다. 다시 시도해주세요.",
                    "REDIS_DELETE_FAILED",
                    "Redis delete failed for key=" + key + ", error=" + e.getMessage(),
                    e
            );
        }

        // 4) 이메일 인증 티켓 발급 (목적을 그대로 사용)
        try {
            String ticket = emailVerifyTicketService.issueTicket(
                    email,
                    purpose,
                    null
            );

            return EmailTicketResponseDto.builder()
                    .ticket(ticket)
                    .build();

        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.EMAIL_TICKET_EXPIRED_OR_INVALID,
                    "인증 처리 중 오류가 발생했습니다. 다시 시도해주세요.",
                    "EMAIL_TICKET_ISSUE_FAILED",
                    "Email ticket issue failed: email=" + email + ", purpose=" + purpose + ", error=" + e.getMessage(),
                    e
            );
        }
    }
}