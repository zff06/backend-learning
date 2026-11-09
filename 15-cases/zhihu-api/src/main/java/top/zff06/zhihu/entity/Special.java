package top.zff06.zhihu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @auther zff06
 * @data 2026/10/20
 * @description Special
 **/
@Data
@TableName("t_special")
public class Special {
    private Long id;
    private String title;
    private String introduction;
    private String banner;
    private String isFollowing;
    private Integer followersCount;
    private Integer viewCount;
    private Long updated;
}
