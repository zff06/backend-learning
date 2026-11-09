package top.mqxu.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.mqxu.security.config.SecurityProperties;
import top.mqxu.security.entity.SysRole;
import top.mqxu.security.entity.SysUser;
import top.mqxu.security.mapper.SysRoleMapper;
import top.mqxu.security.mapper.SysUserMapper;

import java.util.UUID;

/**
 * 按手机号查人，给 SmsCodeAuthenticationProvider 用。
 * <p>
 * 和 DbUserDetailsService 是同级实现，所以必须起不同的 bean 名——
 * AuthenticationManager 那里用 @Qualifier 区分，否则会 NoUniqueBeanDefinitionException。
 * <p>
 * 用户名取手机号：UserDetails 需要一个非空 username，短信用户没有登录名，用手机号占位最简单。
 */
@Slf4j
@Service("phoneUserDetailsService")
@RequiredArgsConstructor
public class PhoneUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserAssembler userAssembler;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));

        if (user == null) {
            if (!properties.getSms().isAutoRegister()) {
                throw new UsernameNotFoundException("手机号未注册：" + mask(phone));
            }
            user = register(phone);
        }
        return userAssembler.toUserDetails(user);
    }

    /**
     * 自动建号。密码列存 {noop} + 随机 UUID——任何人都猜不到，等于这个账号只能用短信进。
     * 真实业务应该在发码前就校验手机号是否已注册，否则知道手机号就能建号。
     */
    private SysUser register(String phone) {
        SysUser user = new SysUser();
        user.setUsername("sms_" + phone);
        user.setPhone(phone);
        user.setPassword("{noop}" + UUID.randomUUID());
        user.setNickname("手机用户" + mask(phone));
        user.setEnabled(true);
        user.setLocked(false);
        sysUserMapper.insert(user);

        SysRole role = new SysRole();
        role.setUserId(user.getId());
        role.setRoleCode("USER");
        role.setGrantType(SysRole.TYPE_ROLE);
        sysRoleMapper.insert(role);

        log.info("短信登录自动建号：id={} phone={}", user.getId(), mask(phone));
        return user;
    }

    /** 日志和异常里不打完整手机号 */
    private String mask(String phone) {
        return phone == null || phone.length() < 7
                ? phone
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
