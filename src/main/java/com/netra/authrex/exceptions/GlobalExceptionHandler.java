package com.netra.authrex.exceptions;

import com.netra.authrex.dtos.ApiResponse;
import com.netra.commons.exceptions.ApplicationException;
import com.netra.commons.exceptions.ExceptionUtil;
import com.netra.commons.exceptions.InvalidEnumException;
import com.netra.commons.exceptions.ServiceUnavailableException;
import com.netra.commons.exceptions.ResourceNotFoundException;
import com.netra.commons.exceptions.ResourceAccessDeniedException;
import com.netra.commons.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    public GlobalExceptionHandler() {

    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return buildErrorResponse(
                null,
                "method_not_allowed",
                request.getMethod() +" not allowed",
                HttpStatus.METHOD_NOT_ALLOWED,
                request,
                ex
        );
    }

    @ExceptionHandler({InvalidEnumException.class})
    public ResponseEntity<ApiResponse<Object>> handleInvalidEnumException(InvalidEnumException e, HttpServletRequest request) {

        return buildErrorResponse(
                e.getAppErrorCode(),
                e.getMessage(),
                null,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e, HttpServletRequest request) {

        String logKey = ExceptionUtil.generateErrorLogKey("ISE");

        return buildErrorResponse(
                "server.error",
                e.getMessage(),
                null,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    // extends logkeyException
    @ExceptionHandler({ApplicationException.class})
    public ResponseEntity<ApiResponse<Object>> handleApplicationException(ApplicationException e, HttpServletRequest request) {

        log.error(String.format("Application Exception : %s", e.getLogKey()), e);

        return buildErrorResponse(
                e.getAppErrorCode(),
                e.getMessage(),
                null,
                e.getStatus(),
                request
        );
    }
    private HttpStatus resolveHttpStatusFrom(Throwable cause) {
        if (cause instanceof RestClientResponseException ex) {
            // RestClientResponseException ex = (RestClientResponseException) cause;
            HttpStatus resolved = HttpStatus.resolve(ex.getRawStatusCode());
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler({ServiceUnavailableException.class})
    public ResponseEntity<ApiResponse<Object>> handleCallNotPermittedException(ServiceUnavailableException e, HttpServletRequest request) {

        return buildErrorResponse(
                e.getAppErrorCode(),
                e.getMessage(),
                null,
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        String resource = e.getResource();

        return buildErrorResponse(
                e.getAppErrorCode(),
                resource.replace(" ", "_").toLowerCase() + "_not_found",
                null,
                HttpStatus.NOT_FOUND,
                request
        );
    }

    // Extends Base Exception
    @ExceptionHandler({ResourceAccessDeniedException.class})
    public ResponseEntity<ApiResponse<Object>> handleAuthorizationException(ResourceAccessDeniedException e,HttpServletRequest request) {

        return buildErrorResponse(
                e.getAppErrorCode(),
                e.getMessage(),
                null,
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(
                ex.getAppErrorCode(),
               ex.getMessage(),
                null,
                HttpStatus.UNAUTHORIZED,
                request
        );
    }

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(String code, String message, List<Error> errors, HttpStatus status, HttpServletRequest request) {
        ApiResponse<Object> response = ApiResponse.error(code, message, errors, request.getRequestURI(), TraceIdFilter.getTraceId());
        return new ResponseEntity<>(response, status);
    }

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(BindingResult bindingResult, String code, String message, HttpStatus status, HttpServletRequest request, Exception ex) {
        List<Error> errorList = null;

        if (bindingResult != null) {
            errorList = bindingResult
                    .getFieldErrors()
                    .stream()
                    .map(fieldError -> new Error(fieldError.getField(), ex))
                    .toList();
        }

        ApiResponse<Object> response = ApiResponse.error(code, message, errorList, request.getRequestURI(), TraceIdFilter.getTraceId());
        return new ResponseEntity<>(response, status);
    }

}

