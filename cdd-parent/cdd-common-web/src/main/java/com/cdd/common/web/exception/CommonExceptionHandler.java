package com.cdd.common.web.exception;

import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorCode().getCode()))
                .body(ApiResponses.failure(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : CommonErrorCode.VALIDATION_FAILED.getMessage();
        return ResponseEntity.status(resolveStatus(CommonErrorCode.VALIDATION_FAILED.getCode()))
                .body(ApiResponses.failure(CommonErrorCode.VALIDATION_FAILED, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : CommonErrorCode.BAD_REQUEST.getMessage();
        return ResponseEntity.status(resolveStatus(CommonErrorCode.BAD_REQUEST.getCode()))
                .body(ApiResponses.failure(CommonErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity.status(resolveStatus(CommonErrorCode.VALIDATION_FAILED.getCode()))
                .body(ApiResponses.failure(CommonErrorCode.VALIDATION_FAILED, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.systemError());
    }

    private HttpStatus resolveStatus(int code) {
        if (code >= 40000 && code < 40100) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 40100 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 40400 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 40900 && code < 41000) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
