package com.dearwith.dearwith_backend.notification.dto;

public record UnreadExistsResponseDto (
        boolean exists,
        Long unreadCount
){
}
