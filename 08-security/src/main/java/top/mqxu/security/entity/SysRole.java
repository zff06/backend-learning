package top.mqxu.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号的角色与权限点，对应 sec_role。
 * <p>
 * 一张表同时存两类东西，靠 grant_type 区分：ROLE 是角色，PERM 是权限点。
 * 分成两张表也能做，但装配 UserDetails 要查两遍，这里合在一起省一次查询。
 */
@Data
@TableName("sec_role")
public class SysRole {

    public static final String TYPE_ROLE = "ROLE";
    public static final String TYPE_PERM = "PERM";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 角色码或权限点字符串，如 ADMIN、ARTICLE_DELETE */
    private String roleCode;

    /** ROLE 或 PERM */
    private String grantType;

    /**
     * 转成 Spring Security 的权限字符串。
     * <p>
     * hasRole("ADMIN") 实际比对的是 ROLE_ADMIN，前缀就在这里补。
     * 这一行 if 写错的后果是静默的：hasRole 永远不通过，日志里只有一行 403。
     * <p>
     * grantType 为空时按角色处理——库里有默认值 'ROLE'，但手工 insert 时可能漏掉。
     */
    public String toAuthority() {
        return TYPE_PERM.equals(grantType) ? roleCode : "ROLE_" + roleCode;
    }
}
