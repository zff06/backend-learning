package top.zff06.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zff06.security.common.BusinessException;
import top.zff06.security.common.ErrorCode;
import top.zff06.security.dto.CreateUserRequest;
import top.zff06.security.entity.User;
import top.zff06.security.mapper.RoleMapper;
import top.zff06.security.mapper.UserMapper;
import top.zff06.security.mapper.UserRoleMapper;
import top.zff06.security.model.Role;
import top.zff06.security.vo.UserProfile;

import java.util.List;

/**
 * 账号管理：只有管理员能进到这里
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 账号列表，视图里没有密码字段 */
    public List<UserProfile> list() {
        return userMapper.selectList(null).stream().map(this::toProfile).toList();
    }

    /** 新增账号：密码入库前加密，角色关联两张表都要写 */
    @Transactional
    public UserProfile create(CreateUserRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEnabled(1);
        userMapper.insert(user);
        for (String roleCode : request.getRoles()) {
            if (Role.fromCode(roleCode) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "未知角色：" + roleCode);
            }
            userRoleMapper.insertUserRole(user.getId(), roleCode);
        }
        return toProfile(user);
    }

    /** 删除账号：不能删自己，不存在给 30003，关联表一并清 */
    @Transactional
    public void delete(String currentUsername, String targetUsername) {
        if (currentUsername.equals(targetUsername)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, targetUsername));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRoleMapper.deleteByUserId(user.getId());
        userMapper.deleteById(user.getId());
    }

    private UserProfile toProfile(User user) {
        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        List<String> authorities = new java.util.ArrayList<>();
        for (String roleCode : roles) {
            authorities.add("ROLE_" + roleCode);
            Role role = Role.fromCode(roleCode);
            if (role != null) {
                authorities.addAll(role.getAuthorities());
            }
        }
        return UserProfile.builder()
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(roles)
                .authorities(authorities)
                .enabled(user.getEnabled() == 1)
                .build();
    }
}
