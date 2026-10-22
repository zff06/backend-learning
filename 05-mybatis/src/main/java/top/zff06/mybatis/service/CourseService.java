package top.zff06.mybatis.service;

import com.baomidou.mybatisplus.spring.service.IService;
import top.zff06.mybatis.entity.Course;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description CourseService
 **/
public interface CourseService extends IService<Course> {
    boolean saveCourses(List<Course> courses);

    /** 按主键批量删除，底层是 removeByIds，t_course 没有逻辑删除列，这里是物理删除 */
    boolean removeCourses(List<Long> ids);

    /** 查询全部，按 id 升序 */
    List<Course> listCourses();
}
