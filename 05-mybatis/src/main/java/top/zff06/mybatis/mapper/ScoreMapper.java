package top.zff06.mybatis.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.zff06.mybatis.entity.Score;
import top.zff06.mybatis.vo.ClassStatVO;
import top.zff06.mybatis.vo.CourseStatVO;
import top.zff06.mybatis.vo.ScoreSheetVO;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/21
 * @description ScoreMapper
 **/
@Mapper
public interface ScoreMapper extends BaseMapper<Score> {
    @Select("""
            SELECT s.id    AS scoreId,
                   st.name AS studentName,
                   c.name  AS courseName,
                   s.score AS score
            FROM t_score s
                     JOIN t_student st ON st.id = s.student_id AND st.deleted = 0
                     JOIN t_course c ON c.id = s.course_id
            WHERE s.deleted = 0
              AND s.student_id = #{studentId}
            ORDER BY s.id
            """)
    List<ScoreSheetVO> selectScoreSheet(@Param("studentId") Long studentId);

    /**
     * 动手练习 2：分页版成绩单。
     * 第一个参数是 IPage，分页插件会把它翻译成 LIMIT，并自动追加一条 COUNT 查总数；
     * 手写 SQL 不走 @TableLogic，两处 deleted = 0 必须自己写
     */
    @Select("""
            SELECT s.id    AS scoreId,
                   st.name AS studentName,
                   c.name  AS courseName,
                   s.score AS score
            FROM t_score s
                     JOIN t_student st ON st.id = s.student_id AND st.deleted = 0
                     JOIN t_course c ON c.id = s.course_id
            WHERE s.deleted = 0
              AND s.student_id = #{studentId}
            ORDER BY s.id
            """)
    IPage<ScoreSheetVO> selectScoreSheetPage(IPage<ScoreSheetVO> page, @Param("studentId") Long studentId);

    /**
     * 动手练习 1：每门课的平均分，带课程名和选课人数。
     * t_score 按 course_id 分组算聚合，再 join t_course 取课程名
     */
    @Select("""
            SELECT c.id                  AS courseId,
                   c.name                AS courseName,
                   COUNT(*)              AS studentCount,
                   ROUND(AVG(s.score),2) AS avgScore
            FROM t_score s
                     JOIN t_course c ON c.id = s.course_id
            WHERE s.deleted = 0
            GROUP BY c.id, c.name
            ORDER BY c.id
            """)
    List<CourseStatVO> selectCourseStat();

    /**
     * 动手练习 3：每个班的平均分。
     * 成绩表 join 学生表按 class_name 分组；st.deleted = 0 把已逻辑删除的赵雪排除在统计外
     */
    @Select("""
            SELECT st.class_name         AS className,
                   COUNT(*)              AS studentCount,
                   ROUND(AVG(s.score),2) AS avgScore
            FROM t_score s
                     JOIN t_student st ON st.id = s.student_id AND st.deleted = 0
            WHERE s.deleted = 0
            GROUP BY st.class_name
            ORDER BY avgScore DESC
            """)
    List<ClassStatVO> selectClassStat();
}
