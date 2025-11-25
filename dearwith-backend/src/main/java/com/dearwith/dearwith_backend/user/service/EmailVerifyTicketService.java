package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.dto.EmailVerificationPurpose;
import com.dearwith.dearwith_backend.user.dto.EmailVerifyPayload;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerifyTicketService {

    private final Cache<String, EmailVerifyPayload> emailVerifyTicketCache;

    /**
     * 이메일 인증이 성공했을 때(코드 검증 완료 후) 티켓 발급
     */
    public String issueTicket(
            String email,
            EmailVerificationPurpose purpose,
            @Nullable UUID userId
    ) {
        if (email == null || email.isBlank()) {
            throw BusinessException.withMessage(
                    ErrorCode.INVALID_INPUT,
                    "이메일 값이 비어 있습니다."
            );
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

        emailVerifyTicketCache.put(ticket, payload);
        return ticket;
    }

    /**
     * 티켓 내용 조회(소비 X) – 디버깅/모니터링용
     */
    @Nullable
    public EmailVerifyPayload peek(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        return emailVerifyTicketCache.getIfPresent(ticket);
    }

    /**
     * 티켓을 소비하면서 검증 (ownerUserId 필요 없는 경우)
     */
    public EmailVerifyPayload confirmAndConsume(String ticket) {
        return confirmAndConsume(ticket, null);
    }

    /**
     * 티켓을 소비하면서 검증 (ownerUserId가 필요한 경우)
     */
    public EmailVerifyPayload confirmAndConsume(String ticket, @Nullable UUID requesterUserId) {

        if (ticket == null || ticket.isBlank()) {
            throw BusinessException.of(ErrorCode.EMAIL_TICKET_EXPIRED_OR_INVALID);
        }

        EmailVerifyPayload payload = emailVerifyTicketCache.getIfPresent(ticket);
        if (payload == null) {
            throw BusinessException.of(ErrorCode.EMAIL_TICKET_EXPIRED_OR_INVALID);
        }

        if (payload.userId() != null && requesterUserId != null) {
            if (!payload.userId().equals(requesterUserId)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.EMAIL_TICKET_NOT_OWNER,
                        null,
                        "EMAIL_TICKET_NOT_OWNER"
                );
            }
        }

        emailVerifyTicketCache.invalidate(ticket);
        return payload;
    }

    /**
     * 이메일/목적까지 함께 검증하고 싶은 경우 편의 메서드
     */
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

    public long size() {
        return emailVerifyTicketCache.estimatedSize();
    }
}