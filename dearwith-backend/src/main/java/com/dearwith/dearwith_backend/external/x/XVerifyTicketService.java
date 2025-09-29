package com.dearwith.dearwith_backend.external.x;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class XVerifyTicketService {

    private final Cache<String, XVerifyPayload> xVerifyTicketCache;

    public String issueTicket(@Nullable UUID ownerAccountId,
                              String xId, String handle, String name, boolean verified) {
        String ticket = UUID.randomUUID().toString();
        XVerifyPayload payload = new XVerifyPayload(
                xId, handle, name, verified, ownerAccountId, Instant.now()
        );
        xVerifyTicketCache.put(ticket, payload);
        return ticket;
    }


    /** (선택) 오버로드: owner 없이 발급 */
    public String issueTicket(String xId, String handle, String name, boolean verified) {
        return issueTicket(null, xId, handle, name, verified);
    }

    public XVerifyPayload peek(String ticket) {
        return xVerifyTicketCache.getIfPresent(ticket); // 삭제하지 않고 조회만
    }

    /**
     * 티켓 확인 + 소모(1회용). owner가 설정되어 있으면 일치 검증.
     * - 없거나 만료: 400 INVALID_TICKET
     * - 소유자 불일치: 403 TICKET_OWNERSHIP
     */
    public XVerifyPayload confirmAndConsume(String ticket, @Nullable UUID requesterUserId) {
        XVerifyPayload payload = xVerifyTicketCache.getIfPresent(ticket);
        if (payload == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_TICKET: 티켓이 없거나 만료되었습니다."
            );
        }

        // 소유자 검증 (payload에 ownerAccountId가 있을 때만)
        UUID owner = payload.ownerUserId();
        if (owner != null && requesterUserId != null && !owner.equals(requesterUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "TICKET_OWNERSHIP: 티켓 소유자가 아닙니다."
            );
        }

        // 1회용 소모
        xVerifyTicketCache.invalidate(ticket);
        return payload;
    }

    /** (선택) 오버로드: 소유자 검증 없이 사용 */
    public XVerifyPayload confirmAndConsume(String ticket) {
        return confirmAndConsume(ticket, null);
    }

    /** (디버그) 현재 캐시 크기 */
    public long size() {
        return xVerifyTicketCache.estimatedSize();
    }
}
