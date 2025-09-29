package com.dearwith.dearwith_backend.external.x;


import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record XVerifyPayload(
        String xId,
        String xHandle,
        String xName,
        boolean verified,
        @Nullable UUID ownerUserId,
        Instant issuedAt
) {}
