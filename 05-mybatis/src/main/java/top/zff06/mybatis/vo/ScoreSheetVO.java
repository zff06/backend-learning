package top.zff06.mybatis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @auther zff06
 * @data 2026/10/21
 * @description ScoreSheetV0
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSheetVO{
    private Long scoreId;
    private String studentName;
    private String courseName;
    private BigDecimal score;
}
