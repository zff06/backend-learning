package top.zff06.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.mapper.CourseMapper;
import top.zff06.mybatis.service.CourseService;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description CourseServiceImpl
 **/
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveCourses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return false;
        }
        return saveBatch(courses);
    }

    @Override
    public boolean removeCourses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    public List<Course> listCourses() {
        return list(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId));
    }
}
