package top.mqxu.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import top.mqxu.security.entity.SysRole;
import top.mqxu.security.entity.SysUser;
import top.mqxu.security.mapper.SysRoleMapper;

import java.util.List;

/**
 * 把库里的 SysUser + 若干 SysRole 装配成 Security 认识的 UserDetails。
 * <p>
 * 密码登录与短信登录共用这一份，避免两条路各写一遍、其中一条漏掉账号状态检查。
 */
@Component
@RequiredArgsConstructor
public class UserAssembler {

    private final SysRoleMapper sysRoleMapper;

    public List<String> authoritiesOf(Long userId) {
        return sysRoleMapper.selectByUserId(userId).stream()
                .map(SysRole::toAuthority)
                .toList();
    }

    /**
     * .disabled() 与 .accountLocked() 是账号状态的来源。
     * DaoAuthenticationProvider 会在校验密码之前检查这两个标记：
     * 禁用抛 DisabledException，锁定抛 LockedException。
     */
    public UserDetails toUserDetails(SysUser user) {
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authoritiesOf(user.getId()).toArray(String[]::new))
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .accountLocked(Boolean.TRUE.equals(user.getLocked()))
                .build();
    }
}
