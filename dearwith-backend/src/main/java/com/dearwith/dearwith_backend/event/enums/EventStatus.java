package com.dearwith.dearwith_backend.event.enums;

public enum EventStatus {
    DRAFT,        // 임시저장
    PENDING_APPROVAL, // 승인대기
    SCHEDULED,    // 시작전
    IN_PROGRESS,  // 진행중
    ENDED,        // 종료
    TBD           // 미정
}
