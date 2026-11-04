package top.zff06.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.zff06.security.mapper.RoleMapper;
import top.zff06.security.mapper.UserMapper;
import top.zff06.security.model.Account;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.entity.User;

import java.util.List;

/**
 * 从库里把"账号 + 角色"读出来，拼成安全框架认识的用户对象
 */
@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        Account account = new Account();
        account.setId(user.getId());
        account.setUsername(user.getUsername());
        account.setPassword(user.getPassword());
        account.setNickname(user.getNickname());
        account.setEnabled(user.getEnabled() == 1);
        account.setRoles(roles);
        return new SecurityUser(account);
    }
}
