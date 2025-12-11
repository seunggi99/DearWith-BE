package com.dearwith.dearwith_backend.common.log;

import com.dearwith.dearwith_backend.logging.context.LogContext;
import com.dearwith.dearwith_backend.logging.context.LogContextHolder;
import com.dearwith.dearwith_backend.logging.entity.BusinessLog;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.logging.enums.LogLevel;
import com.dearwith.dearwith_backend.logging.repository.BusinessLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessLogService {

    private final BusinessLogRepository businessLogRepository;
    private final ObjectMapper objectMapper;

    /* ===========================
     *  편의 메서드 (INFO / ERROR)
     * =========================== */

    public void info(
            BusinessLogCategory category,
            String action,
            UUID actorUserId,
            String targetType,
            String targetId,
            String message,
            Map<String, Object> details
    ) {
        saveLog(category, LogLevel.INFO, action, actorUserId, targetType, targetId, message, details, null);
    }

    public void warn(
            BusinessLogCategory category,
            String action,
            UUID actorUserId,
            String targetType,
            String targetId,
            String message,
            Map<String, Object> details
    ) {
        saveLog(category, LogLevel.WARN, action, actorUserId, targetType, targetId, message, details, null);
    }

    public void error(
            BusinessLogCategory category,
            String action,
            UUID actorUserId,
            String targetType,
            String targetId,
            String message,
            Map<String, Object> details,
            Throwable throwable
    ) {
        saveLog(category, LogLevel.ERROR, action, actorUserId, targetType, targetId, message, details, throwable);
    }

    /* ===========================
     *  공통 저장 로직
     * =========================== */

    private void saveLog(
            BusinessLogCategory category,
            LogLevel level,
            String action,
            UUID actorUserId,
            String targetType,
            String targetId,
            String message,
            Map<String, Object> details,
            Throwable throwable
    ) {
        try {
            LogContext ctx = LogContextHolder.get();

            UUID finalActorUserId = actorUserId;
            if (finalActorUserId == null && ctx != null) {
                finalActorUserId = ctx.getUserId();
            }

            String detailJson = toJsonSafe(details);
            String stackTrace = throwable != null ? getStackTrace(throwable) : null;

            BusinessLog logEntity = BusinessLog.builder()
                    .category(category)
                    .level(level)
                    .action(action)
                    .message(message)

                    .actorUserId(finalActorUserId)
                    .actorIp(ctx != null ? ctx.getRemoteIp() : null)
                    .userAgent(ctx != null ? ctx.getUserAgent() : null)

                    .requestUri(ctx != null ? ctx.getRequestUri() : null)
                    .httpMethod(ctx != null ? ctx.getHttpMethod() : null)
                    .traceId(ctx != null ? ctx.getTraceId() : null)

                    .targetType(targetType)
                    .targetId(targetId)

                    .detailJson(detailJson)
                    .errorStackTrace(stackTrace)
                    .build();

            businessLogRepository.save(logEntity);

            // 애플리케이션 로그에도 함께 출력(원하면 포맷 줄이거나 제거)
            if (level == LogLevel.ERROR) {
                log.error("[BUSINESS-ERROR] category={} action={} message={} traceId={}",
                        category, action, message, logEntity.getTraceId(), throwable);
            } else if (level == LogLevel.WARN) {
                log.warn("[BUSINESS-WARN] category={} action={} message={} traceId={}",
                        category, action, message, logEntity.getTraceId());
            } else {
                log.info("[BUSINESS-INFO] category={} action={} message={} traceId={}",
                        category, action, message, logEntity.getTraceId());
            }

        } catch (Exception e) {
            // 로그 저장 실패로 비즈니스 흐름 깨지지 않도록
            log.error("Failed to save business log. originalAction={} originalMessage={}", action, message, e);
        }
    }

    private String toJsonSafe(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize business log details to JSON. details={}", details, e);
            return null;
        }
    }

    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter(2048);
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}