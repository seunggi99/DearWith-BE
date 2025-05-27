package com.dearwith.dearwith_backend.common.advice;

import com.dearwith.dearwith_backend.common.exception.InvalidVerificationCodeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.dearwith.dearwith_backend.common.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCode(InvalidVerificationCodeException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_CODE", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

}