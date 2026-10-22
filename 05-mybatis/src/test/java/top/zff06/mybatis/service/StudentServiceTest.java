package top.zff06.mybatis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import top.zff06.mybatis.entity.Score;
import top.zff06.mybatis.mapper.ScoreMapper;
import top.zff06.mybatis.vo.ClassStatVO;
import top.zff06.mybatis.vo.CourseStatVO;
import top.zff06.mybatis.vo.ScoreSheetVO;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生成绩查询测试：分步查与 join 查逐字段对比、三道动手练习的结果断言
 * <p>
 * 每个用例执行前都用 schema.sql + data.sql 重建表并写入示例数据，
 * 与 data.sql 完全一致：张伟(1) 三条成绩 / 赵雪(6) deleted=1，其成绩(90、85)仍在 t_score 里
 */
@SpringBootTest
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ScoreMapper scoreMapper;

    /** 用来绕过 MyBatis-Plus，直接改物理行 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<ScoreSheetVO> EXPECTED_SHEET = List.of(
            new ScoreSheetVO(1L, "张伟", "后端工程化开发", new BigDecimal("88.50")),
            new ScoreSheetVO(2L, "张伟", "前端工程化开发", new BigDecimal("92.00")),
            new ScoreSheetVO(3L, "张伟", "Web 应用系统综合实训", new BigDecimal("76.00")));

    private void assertSameSheet(List<ScoreSheetVO> actual) {
        assertEquals(EXPECTED_SHEET.size(), actual.size());
        for (int i = 0; i < EXPECTED_SHEET.size(); i++) {
            ScoreSheetVO expected = EXPECTED_SHEET.get(i);
            ScoreSheetVO row = actual.get(i);
            assertEquals(expected.getScoreId(), row.getScoreId(), "第 " + i + " 行 scoreId 不一致");
            assertEquals(expected.getStudentName(), row.getStudentName(), "第 " + i + " 行 studentName 不一致");
            assertEquals(expected.getCourseName(), row.getCourseName(), "第 " + i + " 行 courseName 不一致");
            assertEquals(0, expected.getScore().compareTo(row.getScore()),
                    "第 " + i + " 行 score 不一致（BigDecimal 用 compareTo，忽略精度差）");
        }
    }

    @Test
    @DisplayName("成绩单：分步查与 join 查的返回值逐字段一致")
    void scoreSheet_stepsAndJoinShouldMatch() {
        assertSameSheet(studentService.scoreSheetBySteps(1L));
        assertSameSheet(studentService.scoreSheetByJoin(1L));
    }

    @Test
    @DisplayName("成绩单：join 查不到已逻辑删除学生的成绩（赵雪）")
    void scoreSheet_joinShouldExcludeDeletedStudent() {
        // 赵雪(id=6) deleted=1，join SQL 里的 st.deleted=0 应把她的成绩挡掉
        assertTrue(studentService.scoreSheetByJoin(6L).isEmpty(),
                "逻辑删除学生的成绩不应出现在 join 结果里");
        // 分步查询第一步 getById 返回 null，同样拿不到
        assertTrue(studentService.scoreSheetBySteps(6L).isEmpty());
    }

    @Test
    @DisplayName("成绩单：分步查会过滤已逻辑删除的成绩行，与 join 行为一致")
    void scoreSheet_stepsShouldExcludeDeletedScore() {
        // 直接把张伟的 course_id=2 那条成绩标记删除（绕过逻辑删除拦截，模拟真实状态）
        jdbcTemplate.update("UPDATE t_score SET deleted = 1 WHERE id = 2");

        List<ScoreSheetVO> steps = studentService.scoreSheetBySteps(1L);
        List<ScoreSheetVO> join = studentService.scoreSheetByJoin(1L);

        assertEquals(2, steps.size(), "分步查应自动带上 deleted=0（Score 实体的 @TableLogic）");
        assertEquals(2, join.size(), "join 查手写的 s.deleted=0 应过滤同一行");
        assertEquals(steps.size(), join.size());
        assertTrue(steps.stream().noneMatch(v -> v.getScoreId() == 2L));
        assertTrue(join.stream().noneMatch(v -> v.getScoreId() == 2L));
    }

    @Test
    @DisplayName("练习 1：每门课的平均分、选课人数（对照 data.sql 手算值）")
    void courseStat_shouldMatchSeedData() {
        List<CourseStatVO> stats = studentService.courseStat();

        assertEquals(3, stats.size());
        CourseStatVO c1 = stats.get(0);
        assertEquals("后端工程化开发", c1.getCourseName());
        assertEquals(6L, c1.getStudentCount(), "88.5+95+58+72+66+90 共 6 条");
        assertEquals(0, new BigDecimal("78.25").compareTo(c1.getAvgScore()), "469.5 / 6 = 78.25");
        CourseStatVO c2 = stats.get(1);
        assertEquals("前端工程化开发", c2.getCourseName());
        assertEquals(5L, c2.getStudentCount());
        assertEquals(0, new BigDecimal("82.00").compareTo(c2.getAvgScore()));
        CourseStatVO c3 = stats.get(2);
        assertEquals("Web 应用系统综合实训", c3.getCourseName());
        assertEquals(4L, c3.getStudentCount());
        assertEquals(0, new BigDecimal("75.00").compareTo(c3.getAvgScore()));
    }

    @Test
    @DisplayName("练习 2：分页成绩单，total/pages/records 正确")
    void scoreSheetPage_shouldPaginate() {
        var page = studentService.scoreSheetPage(1L, 1, 2);

        assertEquals(3, page.getTotal());
        assertEquals(2, page.getPages());
        assertEquals(2, page.getRecords().size());
        assertEquals(1, page.getRecords().get(0).getScoreId());

        var second = studentService.scoreSheetPage(1L, 2, 2);
        assertEquals(1, second.getRecords().size());
        assertEquals(3L, second.getRecords().get(0).getScoreId());
    }

    @Test
    @DisplayName("练习 3：班级平均分，已逻辑删除的赵雪不参与统计")
    void classStat_shouldExcludeDeletedStudent() {
        List<ClassStatVO> stats = studentService.classStat();

        // 按 avgScore 降序：软件2411(88.67) → 软件2421(77.00) → 软件2412(63.70)
        assertEquals(3, stats.size());
        ClassStatVO first = stats.get(0);
        assertEquals("软件2411", first.getClassName());
        assertEquals(6L, first.getStudentCount());
        assertEquals(0, new BigDecimal("88.67").compareTo(first.getAvgScore()));

        ClassStatVO second = stats.get(1);
        assertEquals("软件2421", second.getClassName(), "赵雪(90、85)被 st.deleted=0 排除，只剩陈晨两条");
        assertEquals(2L, second.getStudentCount(), "66+88=154，平均 77.00");
        assertEquals(0, new BigDecimal("77.00").compareTo(second.getAvgScore()));

        ClassStatVO third = stats.get(2);
        assertEquals("软件2412", third.getClassName());
        assertEquals(5L, third.getStudentCount());
        assertEquals(0, new BigDecimal("63.70").compareTo(third.getAvgScore()));
    }

    @Test
    @DisplayName("实体：Score 带逻辑删除字段，deleteById 发的是 UPDATE")
    void scoreTableLogic_shouldBeConfigured() {
        Score score = scoreMapper.selectById(1L);
        scoreMapper.deleteById(1L);

        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM t_score WHERE id = 1", Integer.class);
        assertEquals(1, deleted, "@TableLogic 生效：物理行还在，deleted 被置 1");
        assertTrue(scoreMapper.selectById(1L) == null, "后续查询自动过滤 deleted=1 的行");
    }
}
