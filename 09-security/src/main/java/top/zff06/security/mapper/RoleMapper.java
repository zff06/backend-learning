package top.zff06.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.zff06.security.model.UserRoleEntry;

import java.util.List;

/**
 * 角色相关查询：角色字典表和关联表都不需要完整 CRUD，只按需查
 */
@Mapper
public interface RoleMapper {

    /** 查某用户拥有的全部角色编码 */
    @Select("""
            SELECT r.code
            FROM sec_role r
            JOIN sec_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleCodesByUserId(Long userId);

    /** 查全部用户与角色的关联（管理端批量用不上时也保留基础能力） */
    @Select("""
            SELECT ur.user_id AS userId, r.code AS roleCode
            FROM sec_user_role ur
            JOIN sec_role r ON r.id = ur.role_id
            """)
    List<UserRoleEntry> findAllUserRoleEntries();
}
