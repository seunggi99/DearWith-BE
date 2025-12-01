package com.dearwith.dearwith_backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import java.util.stream.Collectors;

import static com.dearwith.dearwith_backend.common.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*──────────────────────────────────────────────
     | 1. BusinessException (Custom)
     *──────────────────────────────────────────────*/
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();
        String path = request.getRequestURI();

        // 사용자 메시지: override > 기본
        String userMessage = ex.getUserMessage() != null
                ? ex.getUserMessage()
                : errorCode.getMessage();

        // 프론트용 detail
        String detail = ex.getDetail();

        // 서버 로그용 logDetail 우선
        String logDetail = ex.getLogDetail() != null
                ? ex.getLogDetail()
                : (detail != null ? detail : errorCode.getMessage());

        log.warn("[BusinessException] code={} message={} detail={} logDetail={} path={}",
                errorCode.name(), userMessage, detail, logDetail, path, ex);

        ErrorResponse body = ErrorResponse.of(errorCode, userMessage, detail, path);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }


    /*──────────────────────────────────────────────
     | 2. Authentication / Security
     *──────────────────────────────────────────────*/
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex,
                                                                HttpServletRequest request) {
        ErrorCode errorCode = INVALID_EMAIL;
        log.warn("[UsernameNotFound] {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.of(errorCode, errorCode.getMessage(), null, request.getRequestURI());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        ErrorCode errorCode = INVALID_PASSWORD;
        log.warn("[BadCredentials] {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.of(errorCode, errorCode.getMessage(), null, request.getRequestURI());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }


    /*──────────────────────────────────────────────
     | 3. Validation 관련
     *──────────────────────────────────────────────*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        String path = request.getRequestURI();

        String detail = e.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값 검증에 실패했습니다.");

        log.warn("[ValidationException] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(VALIDATION_FAILED, detail, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {

        String path = request.getRequestURI();

        String detail = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("요청 값 검증에 실패했습니다.");

        log.warn("[ConstraintViolationException] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(VALIDATION_FAILED, detail, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex,
                                                             HttpServletRequest request) {

        String path = request.getRequestURI();

        String detail = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[BindException] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(VALIDATION_FAILED, detail, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }


    /*──────────────────────────────────────────────
     | 4. 요청 형식 / 파라미터 오류
     *──────────────────────────────────────────────*/
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseException(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        String path = request.getRequestURI();
        String detail = (e.getMostSpecificCause() != null)
                ? e.getMostSpecificCause().getMessage()
                : "요청 본문을 읽을 수 없습니다.";

        log.warn("[JsonParseException] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_FORMAT, INVALID_FORMAT.getMessage(), detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        String path = request.getRequestURI();
        String requiredType = (e.getRequiredType() != null) ? e.getRequiredType().getSimpleName() : "";

        String detail = String.format(
                "변수 '%s'에 대한 값 '%s'를(을) %s 타입으로 변환할 수 없습니다.",
                e.getName(), e.getValue(), requiredType
        );

        log.warn("[TypeMismatch] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_PATH_VARIABLE, INVALID_PATH_VARIABLE.getMessage(), detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MissingPathVariableException.class })
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            Exception e, HttpServletRequest request) {

        String path = request.getRequestURI();
        String detail = e.getMessage();

        log.warn("[MissingParameter] detail={} path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_REQUEST, INVALID_REQUEST.getMessage(), detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return ErrorResponse.of(
                ErrorCode.NOT_FOUND,
                "지원하지 않는 API입니다.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        String supportedMethods = (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty())
                ? ex.getSupportedHttpMethods().stream()
                .map(HttpMethod::name)
                .collect(Collectors.joining(", "))
                : "요청 가능한 메서드";

        String detail = String.format(
                "해당 API는 %s 메서드 요청을 지원하지 않습니다. 지원 메서드: %s",
                ex.getMethod(),
                supportedMethods
        );

        log.warn("[MethodNotSupported] method={} supported={} path={}",
                ex.getMethod(), supportedMethods, path);

        ErrorResponse body = ErrorResponse.of(
                INVALID_REQUEST,
                INVALID_REQUEST.getMessage(),
                detail,
                path
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body);
    }

    /*──────────────────────────────────────────────
     | 5. RuntimeException
     *──────────────────────────────────────────────*/
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        ErrorCode errorCode = OPERATION_FAILED;

        log.error("[RuntimeException] path={}", path, ex);

        ErrorResponse body = ErrorResponse.of(errorCode, errorCode.getMessage(), null, path);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }


    /*──────────────────────────────────────────────
     | 6. 마지막 fallback 예외
     *──────────────────────────────────────────────*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        ErrorCode errorCode = OPERATION_FAILED;

        log.error("[UnhandledException] path={}", path, ex);

        ErrorResponse body = ErrorResponse.of(errorCode, errorCode.getMessage(), null, path);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }
}