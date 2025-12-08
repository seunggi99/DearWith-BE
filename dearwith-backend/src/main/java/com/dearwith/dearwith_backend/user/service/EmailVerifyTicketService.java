package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.dto.EmailVerificationPurpose;
import com.dearwith.dearwith_backend.user.dto.EmailVerifyPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerifyTicketService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TICKET_TTL = Duration.ofMinutes(10);

    private String key(String ticket) {
        return "email:verify:ticket:" + ticket;
    }

    private String toJson(EmailVerifyPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize EmailVerifyPayload", e);
        }
    }

    private EmailVerifyPayload fromJson(String json) {
        try {
            return objectMapper.readValue(json, EmailVerifyPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize EmailVerifyPayload", e);
        }
    }

    /*──────────────────────────────────────────────
     | 티켓 발급
     *──────────────────────────────────────────────*/
    public String issueTicket(String email, EmailVerificationPurpose purpose, @Nullable UUID userId) {

        if (email == null || email.isBlank()) {
            throw BusinessException.withMessage(ErrorCode.INVALID_INPUT, "이메일 값이 비어 있습니다.");
        }
        if (purpose == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이메일 인증 목적이 필요합니다.",
                    "EMAIL_VERIFY_PURPOSE_REQUIRED"
            );
        }

        String ticket = UUID.randomUUID().toString().replace("-", "");

        EmailVerifyPayload payload = new EmailVerifyPayload(
                email,
                purpose,
                userId,
                Instant.now()
        );

        redisTemplate.opsForValue()
                .set(key(ticket), toJson(payload), TICKET_TTL);

        return ticket;
    }

    /*──────────────────────────────────────────────
     | 조회 (소비 X)
     *──────────────────────────────────────────────*/
    @Nullable
    public EmailVerifyPayload peek(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;

        String json = redisTemplate.opsForValue().get(key(ticket));
        return json == null ? null : fromJson(json);
    }

    /*──────────────────────────────────────────────
     | 검증 + 소비 (owner 검사 필요 없는 경우)
     *──────────────────────────────────────────────*/
    public EmailVerifyPayload confirmAndConsume(String ticket) {
        return confirmAndConsume(ticket, null);
    }

    /*──────────────────────────────────────────────
     | 검증 + 소비 (owner 검사 필요한 경우)
     *──────────────────────────────────────────────*/
    public EmailVerifyPayload confirmAndConsume(String ticket, @Nullable UUID requesterUserId) {

        if (ticket == null || ticket.isBlank()) {
            throw BusinessException.of(ErrorCode.EMAIL_TICKET_EXPIRED_OR_INVALID);
        }

        String redisKey = key(ticket);
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            throw BusinessException.of(ErrorCode.EMAIL_TICKET_EXPIRED_OR_INVALID);
        }

        EmailVerifyPayload payload = fromJson(json);

        // 🔐 owner 제한이 필요한 경우
        if (payload.userId() != null && requesterUserId != null) {
            if (!payload.userId().equals(requesterUserId)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.EMAIL_TICKET_NOT_OWNER,
                        null,
                        "EMAIL_TICKET_NOT_OWNER"
                );
            }
        }

        // 🔥 한 번 사용하면 반드시 제거
        redisTemplate.delete(redisKey);

        return payload;
    }

    /*──────────────────────────────────────────────
     | 이메일 & 목적까지 검증하는 편의 메서드
     *──────────────────────────────────────────────*/
    public EmailVerifyPayload confirmForPurposeAndEmail(
            String ticket,
            EmailVerificationPurpose expectedPurpose,
            String expectedEmail
    ) {

        EmailVerifyPayload payload = confirmAndConsume(ticket);

        if (payload.purpose() != expectedPurpose) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.EMAIL_TICKET_WRONG_PURPOSE,
                    null,
                    "EMAIL_TICKET_WRONG_PURPOSE"
            );
        }

        if (!payload.email().equalsIgnoreCase(expectedEmail)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.EMAIL_TICKET_EMAIL_MISMATCH,
                    null,
                    "EMAIL_TICKET_EMAIL_MISMATCH"
            );
        }

        return payload;
    }
}