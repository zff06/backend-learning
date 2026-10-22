package top.zff06.mybatis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 每个班级的统计结果：参考人数、平均分
 *
 * @auther zff06
 * @data 2026/10/21
 * @description ClassStatVO
 **/
@Data
@NoArgsConstructor
public class ClassStatVO {
    private String className;
    /** 参与统计的成绩条数（已排除逻辑删除的学生） */
    private Long studentCount;
    /** 平均分，保留 2 位 */
    private BigDecimal avgScore;
}
