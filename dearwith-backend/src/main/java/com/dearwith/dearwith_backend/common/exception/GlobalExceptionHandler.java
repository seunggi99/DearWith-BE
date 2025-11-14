package com.dearwith.dearwith_backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.util.stream.Collectors;

import static com.dearwith.dearwith_backend.common.exception.ErrorCode.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 (커스텀)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex,
                                                                 HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("비즈니스 예외 발생: {} ({}) detail={} uri={}",
                errorCode.name(),
                ex.getMessage(),
                ex.getDetail(),
                request.getRequestURI(),
                ex
        );

        ErrorResponse response = ErrorResponse.of(
                errorCode,
                ex.getDetail(),
                request.getRequestURI()
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // 이메일(아이디) 없음
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_EMAIL;
        log.warn("이메일 없음: {}", ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.getMessage(), errorCode.name(),null,null,null));
    }

    // 비밀번호 불일치
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_PASSWORD;
        log.warn("비밀번호 불일치: {}", ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.getMessage(), errorCode.name(),null,null,null));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errorMessage, "VALIDATION_ERROR",null,null,null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArg(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_REQUEST", ex.getMessage(),null,null,null);
    }

    // 그 외 런타임 예외
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorCode errorCode = ErrorCode.OPERATION_FAILED;
        log.error("처리되지 않은 런타임 예외 발생", ex);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.getMessage(), errorCode.name(),null,null,null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e,
                                                                    HttpServletRequest request) {

        String path = request.getRequestURI();

        // 첫 번째 필드 에러 메시지만 간단히 사용
        String detail = e.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> String.format("[%s] %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .orElse("요청 값 검증에 실패했습니다.");

        log.warn("[ValidationException] detail={}, path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(VALIDATION_FAILED, detail, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e,
                                                                            HttpServletRequest request) {

        String path = request.getRequestURI();

        String detail = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("요청 값 검증에 실패했습니다.");

        log.warn("[ConstraintViolationException] detail={}, path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(VALIDATION_FAILED, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseException(HttpMessageNotReadableException e,
                                                                  HttpServletRequest request) {

        String path = request.getRequestURI();
        String detail = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : "요청 본문을 읽을 수 없습니다.";

        log.warn("[JsonParseException] detail={}, path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_FORMAT, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {

        String path = request.getRequestURI();
        String requiredType = (e.getRequiredType() != null)
                ? e.getRequiredType().getSimpleName()
                : "요청 타입";

        String detail = String.format("변수 '%s'에 대한 값 '%s'를(을) %s 으로 변환할 수 없습니다.",
                e.getName(), e.getValue(), requiredType);

        log.warn("[MethodArgumentTypeMismatch] detail={}, path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_PATH_VARIABLE, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class
    })
    public ResponseEntity<ErrorResponse> handleMissingParameter(Exception e,
                                                                HttpServletRequest request) {

        String path = request.getRequestURI();
        String detail = e.getMessage();

        log.warn("[MissingParameter] detail={}, path={}", detail, path);

        ErrorResponse body = ErrorResponse.of(INVALID_REQUEST, detail, path);
        return ResponseEntity.status(BAD_REQUEST).body(body);
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("처리되지 않은 예외 발생: {}", ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.OPERATION_FAILED,
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.OPERATION_FAILED.getHttpStatus()).body(response);
    }

}