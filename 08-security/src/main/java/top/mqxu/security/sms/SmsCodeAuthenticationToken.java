package top.mqxu.security.sms;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * "手机号 + 验证码"这种凭证。两个构造器分别对应登录前与登录后。
 * <p>
 * super(null) 必须强转成 Collection：Security 7 的 AbstractAuthenticationToken
 * 有两个接收集合的构造器，直接写 super(null) 编译器无法判断选哪个，
 * 报 reference to ... is ambiguous。
 */
public class SmsCodeAuthenticationToken extends AbstractAuthenticationToken {

    /** 登录前是手机号字符串，登录后是已认证的 principal */
    private final Object principal;

    /** 登录前是验证码，登录后为 null */
    private String credentials;

    /**
     * 登录前：前端提交的手机号与验证码。此时未认证，没有任何权限。
     */
    public SmsCodeAuthenticationToken(String phone, String code) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = phone;
        this.credentials = code;
        setAuthenticated(false);
    }

    /**
     * 认证成功后由 Provider 创建。
     */
    public SmsCodeAuthenticationToken(Object principal,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    /**
     * 认证完成后不允许再改凭证，防止下游篡改上下文。
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
