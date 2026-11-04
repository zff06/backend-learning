package top.zff06.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.zff06.security.entity.User;

/**
 * 关联表写操作：新增账号时写入用户角色，删账号时一并清掉
 */
@Mapper
public interface UserRoleMapper {

    @Insert("INSERT INTO sec_user_role (user_id, role_id) " +
            "SELECT #{userId}, id FROM sec_role WHERE code = #{roleCode}")
    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Delete("DELETE FROM sec_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
