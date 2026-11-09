package top.mqxu.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.mqxu.security.entity.SysUser;
import top.mqxu.security.mapper.SysUserMapper;

/**
 * 按用户名查人，给 DaoAuthenticationProvider 用。
 * <p>
 * 这个 bean 一旦存在，Boot 就不再生成默认用户，
 * 启动日志里也不会再出现 "Using generated security password"。
 * <p>
 * 注意抛出的是 UsernameNotFoundException 而不是 BusinessException：
 * DaoAuthenticationProvider 会把 UsernameNotFoundException 改写成 BadCredentialsException，
 * 从而不泄露"这个用户名到底存不存在"（防用户枚举）。
 */
@Service("dbUserDetailsService")
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final UserAssembler userAssembler;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return userAssembler.toUserDetails(user);
    }
}
