package top.zff06.mybatis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 每门课的统计结果：选课人数、平均分
 *
 * @auther zff06
 * @data 2026/10/21
 * @description CourseStatVO
 **/
@Data
@NoArgsConstructor
public class CourseStatVO {
    private Long courseId;
    private String courseName;
    /** 选课人数（有成绩记录的条数） */
    private Long studentCount;
    /** 平均分，保留 2 位 */
    private BigDecimal avgScore;
}
