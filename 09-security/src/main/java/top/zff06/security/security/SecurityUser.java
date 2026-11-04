package top.zff06.security.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import top.zff06.security.model.Account;
import top.zff06.security.model.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 安全框架认识的用户对象：包一层 Account，不让实体实现 UserDetails
 * getAuthorities 同时返回两类字符串：
 * 角色名带 ROLE_ 前缀（配合 hasRole），权限点原样（配合 hasAuthority）
 */
@Getter
public class SecurityUser implements UserDetails {

    private final Account account;

    public SecurityUser(Account account) {
        this.account = account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        for (String roleCode : account.getRoles()) {
            list.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
            Role role = Role.fromCode(roleCode);
            if (role != null) {
                for (String authority : role.getAuthorities()) {
                    list.add(new SimpleGrantedAuthority(authority));
                }
            }
        }
        return list;
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(account.getEnabled());
    }

    public Long getId() {
        return account.getId();
    }

    public String getNickname() {
        return account.getNickname();
    }

    public List<String> getRoles() {
        return account.getRoles();
    }
}
