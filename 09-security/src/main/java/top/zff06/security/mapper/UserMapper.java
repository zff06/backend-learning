package top.zff06.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.zff06.security.entity.User;

/**
 * 账号表通用 CRUD
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
