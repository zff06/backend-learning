package top.zff06.security.model;

import lombok.Data;

/**
 * 用户与角色关联查询的投影：一列用户 id 对一条角色编码
 */
@Data
public class UserRoleEntry {

    private Long userId;

    private String roleCode;
}
