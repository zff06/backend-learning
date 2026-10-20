package top.zff06.mybatis.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.mapper.CourseMapper;
import top.zff06.mybatis.service.CourseService;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description CourseServiceImpl
 **/
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

}
