package com.dearwith.dearwith_backend.logging.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.logging.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "business_log",
        indexes = {
                @Index(
                        name = "idx_business_log_category_created_at",
                        columnList = "category, created_at"
                ),
                @Index(
                        name = "idx_business_log_user_created_at",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_business_log_target",
                        columnList = "target_type, target_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========= 기본 메타 =========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BusinessLogCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogLevel level;

    /**
     * 수행한 비즈니스 액션(ex. EVENT_CREATE, REVIEW_DELETE, PUSH_SEND 등)
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * 사람이 읽기 쉬운 요약 메시지
     */
    @Column(nullable = false, length = 500)
    private String message;

    // ========= 액터(요청자) 정보 =========

    @Column(columnDefinition = "BINARY(16)")
    private UUID actorUserId;

    @Column(length = 50)
    private String actorIp;

    @Column(length = 300)
    private String userAgent;

    // ========= 요청 정보 =========

    @Column(length = 300)
    private String requestUri;

    @Column(length = 10)
    private String httpMethod;

    /**
     * trace / correlation id (요청 단위로 묶고 싶을 때)
     */
    @Column(length = 100)
    private String traceId;

    // ========= 타겟 리소스 =========

    /**
     * ex) "EVENT", "REVIEW", "USER" 등
     */
    @Column(length = 100)
    private String targetType;

    /**
     * ex) eventId, reviewId 등
     */
    private String targetId;

    // ========= 상세 정보 / 오류 =========

    /**
     * JSON 문자열 (추가 메타데이터)
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String detailJson;

    /**
     * 에러 로그일 때 스택트레이스
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String errorStackTrace;
}