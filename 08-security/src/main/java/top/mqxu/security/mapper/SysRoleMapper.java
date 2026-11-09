package top.mqxu.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.mqxu.security.entity.SysRole;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查某个账号的全部角色与权限点。
     * <p>
     * 不加 ORDER BY：sec_role 表上唯一索引是 (user_id, role_code)，InnoDB 返回顺序不保证。
     * 调用方要断言权限列表，只能用"包含"式断言，不能按下标取。
     */
    @Select("SELECT * FROM sec_role WHERE user_id = #{userId}")
    List<SysRole> selectByUserId(Long userId);
}
