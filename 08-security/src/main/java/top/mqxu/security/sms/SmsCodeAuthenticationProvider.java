package top.mqxu.security.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 认"手机号 + 验证码"这种凭证。
 * <p>
 * 这就是短信登录的正确做法：不是新写一个登录接口绕过 Security，
 * 而是给它增加一种凭证类型。这样五条链的授权规则、账号状态检查、
 * 后面可能加的 @PreAuthorize，全都自动适用。
 */
@Slf4j
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {

    private final SmsCodeService smsCodeService;
    private final UserDetailsService phoneUserDetailsService;

    public SmsCodeAuthenticationProvider(SmsCodeService smsCodeService,
                                         UserDetailsService phoneUserDetailsService) {
        this.smsCodeService = smsCodeService;
        this.phoneUserDetailsService = phoneUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        SmsCodeAuthenticationToken token = (SmsCodeAuthenticationToken) authentication;
        String phone = String.valueOf(token.getPrincipal());
        String code = String.valueOf(token.getCredentials());

        if (!smsCodeService.verify(phone, code)) {
            throw new BadCredentialsException("验证码错误或已过期");
        }

        UserDetails user;
        try {
            user = phoneUserDetailsService.loadUserByUsername(phone);
        } catch (UsernameNotFoundException e) {
            // 不暴露"这个手机号有没有注册过"
            throw new BadCredentialsException("验证码错误或已过期");
        }

        // 账号状态检查必须自己做：DaoAuthenticationProvider 是替密码登录做的，不会替这里做。
        // 漏掉这一段，被禁用或锁定的账号照样能靠短信登进来——
        // 这是自己写 Provider 最容易出的漏洞，因为密码登录那条路框架已经替你做了。
        if (!user.isEnabled()) {
            throw new DisabledException("账号已被禁用");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("账号已被锁定");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("账号已过期");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("凭证已过期");
        }

        log.debug("短信登录成功 phone={} user={}", phone, user.getUsername());

        SmsCodeAuthenticationToken result =
                new SmsCodeAuthenticationToken(user, user.getAuthorities());
        result.setDetails(token.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
