package top.zff06.mybatis.mapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.zff06.mybatis.entity.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Slf4j
class UserMapperTest {
    @Resource
    private UserMapper userMapper;

    @Test
    void testInsert(){
        User user=new User();
        user.setUsername("Mikasa");
        user.setAge(19);
        user.setCreateTime(LocalDateTime.now());
        int result = userMapper.insert(user);
        log.info("{} 条数据插入成功",result);
    }

    @Test
    void testSearch(){
        User user = userMapper.selectById(3);
        log.info("用户信息: {}",user);
    }

    @Test
    void testDelete(){
        int result = userMapper.deleteById(4);
        assertEquals(1,result);
        log.info("{} 条数据删除成功", result);
    }
}