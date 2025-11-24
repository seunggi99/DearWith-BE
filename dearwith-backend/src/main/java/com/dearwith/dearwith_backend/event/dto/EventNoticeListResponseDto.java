package com.dearwith.dearwith_backend.event.dto;

import org.springframework.data.domain.Page;

public record EventNoticeListResponseDto(
        Long eventId,
        boolean writable,
        Page<EventNoticeInfoDto> notices
) { }