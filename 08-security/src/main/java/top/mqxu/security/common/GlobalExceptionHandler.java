package top.mqxu.security.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。这里是"错误响应的第三处实现"——前两处在过滤器链：
 * JsonAuthenticationEntryPoint（401）与 JsonAccessDeniedHandler（403）。
 * <p>
 * 三处必须都写，而且格式一致，否则同一个 403 会因为来源不同给前端两种结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @PreAuthorize 拒绝时抛的是 AuthorizationDeniedException（AccessDeniedException 的子类），
     * 它在 Controller 调用栈里抛出，走不到 ExceptionTranslationFilter，
     * JsonAccessDeniedHandler 接不到。不单独处理会被兜底成 500。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("方法级鉴权拒绝：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseEntity.status(mapStatus(e.getErrorCode()))
                .body(Result.fail(e.getErrorCode().getCode(), e.getMessage()));
    }

    /**
     * 登录接口的兜底。真正的消息翻译在 AuthController 里做（那里能区分 401 与 403），
     * 这里只处理漏网的认证异常。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ErrorCode.UNAUTHORIZED));
    }

    /**
     * @RequestBody 上的 @Valid 校验失败。
     * 注意这一条不会被 /error 覆盖：异常在 Controller 层就被接住了。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidArgument(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * 缺 @RequestParam。短信发码接口漏传 phone 时走这条，返回 400 而不是 500。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少参数：" + e.getParameterName()));
    }

    /**
     * @RequestParam 上的 @Pattern 校验失败（类上必须加 @Validated 才会触发）。
     * 它抛的是 ConstraintViolationException，不是 MethodArgumentNotValidException，
     * 不单独接住就会被兜底成 500——手机号格式错变成"服务器内部错误"，排查时很容易误判。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数约束校验失败：{}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * HandlerMethodValidationException：Spring 6.1+ 对 Controller 方法参数的校验失败会抛这个，
     * 与上面的 ConstraintViolationException 是两条不同的路径，都要接。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("方法参数校验失败：{}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.BAD_REQUEST.getCode(),
                        message.isBlank() ? ErrorCode.BAD_REQUEST.getMessage() : message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(HttpServletRequest request, Exception e) {
        log.error("未处理的异常：{} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * 业务码到 HTTP 状态码的映射。禁用与锁定返回 403 而不是 401——
     * 前者是"你输错了"，后者是"你该去找管理员"，混成一个用户会一直重试密码。
     */
    private HttpStatus mapStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case BAD_CREDENTIALS, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_DISABLED, ACCOUNT_LOCKED, ACCOUNT_EXPIRED, CREDENTIALS_EXPIRED, FORBIDDEN ->
                    HttpStatus.FORBIDDEN;
            case SMS_CODE_ERROR, SMS_SEND_TOO_FREQUENT, SMS_PHONE_NOT_REGISTERED, BAD_REQUEST ->
                    HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
