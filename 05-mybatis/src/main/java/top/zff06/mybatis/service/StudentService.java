package top.zff06.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import top.zff06.mybatis.entity.Student;
import top.zff06.mybatis.vo.ClassStatVO;
import top.zff06.mybatis.vo.CourseStatVO;
import top.zff06.mybatis.vo.ScoreSheetVO;

import java.util.List;
import java.util.Map;

/**
 * @auther zff06
 * @data 2026/10/21
 * @description StudentService
 **/
public interface StudentService extends IService<Student> {
    /**
     * 按 id 查一个学生，查不到返回 null
     */
    Student getStudent(Long id);

    /**
     * 查全部学生
     */
    List<Student> listAll();

    /**
     * 按班级查
     */
    List<Student> listByClass(String className);

    /**
     * 按姓名模糊查
     */
    List<Student> searchByName(String name);

    /**
     * 按年龄区间查，两端都可以不传
     */
    List<Student> listByAgeRange(Integer minAge, Integer maxAge);

    /**
     * 分页查学生
     */
    Page<Student> pageStudents(long current, long size);

    /**
     * 只取姓名这一列
     */
    List<String> listNames();

    /**
     * 统计每个学生的选课门数、总分、平均分
     */
    List<Map<String, Object>> scoreStat();

    /**
     * 查某学生的成绩单：分两次单表查询，在 Java 里拼起来
     */
    List<ScoreSheetVO> scoreSheetBySteps(Long studentId);

    /**
     * 查某学生的成绩单：一条 join SQL 直接查出来
     */
    List<ScoreSheetVO> scoreSheetByJoin(Long studentId);

    /**
     * 动手练习 1：每门课的平均分，带课程名和选课人数
     */
    List<CourseStatVO> courseStat();

    /**
     * 动手练习 2：分页版成绩单
     */
    IPage<ScoreSheetVO> scoreSheetPage(Long studentId, long current, long size);

    /**
     * 动手练习 3：每个班的平均分，不含已逻辑删除的学生
     */
    List<ClassStatVO> classStat();
}