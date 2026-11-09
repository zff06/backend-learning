package top.mqxu.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号，对应 sec_user。
 * <p>
 * enabled 与 locked 是两个独立状态：禁用是"这个账号不允许登录"，锁定是"被临时冻结"。
 * 业务上处理方式不同（前者找管理员开通，后者等自动解锁或重置），所以分两列存。
 */
@Data
@TableName("sec_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，密码登录用它 */
    private String username;

    /** 手机号，短信登录用它；可为空 */
    private String phone;

    /** 带算法前缀的密文，如 {bcrypt}$2a$10$... */
    private String password;

    private String nickname;

    /** false 时登录抛 DisabledException */
    private Boolean enabled;

    /** true 时登录抛 LockedException */
    private Boolean locked;

    private LocalDateTime createTime;
}
