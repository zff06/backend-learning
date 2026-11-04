package top.zff06.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.zff06.security.entity.Task;
import top.zff06.security.model.TaskRow;

import java.util.List;

/**
 * 任务查询：单表走 BaseMapper，带提交人信息的联表走手写 SQL
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    String JOIN_OWNER = """
            SELECT t.id, t.title, t.content, t.owner_id AS ownerId,
                   u.username AS owner, u.nickname AS ownerNickname,
                   t.status, t.score, t.comment, t.submitted_at AS submittedAt, t.reviewed_at AS reviewedAt
            FROM sec_task t
            JOIN sec_user u ON u.id = t.owner_id
            """;

    /** 查全部任务（教师视角），按提交时间倒序 */
    @Select(JOIN_OWNER + " ORDER BY t.submitted_at DESC")
    List<TaskRow> selectAllWithOwner();

    /** 查某人的全部任务（学生视角） */
    @Select(JOIN_OWNER + " WHERE t.owner_id = #{ownerId} ORDER BY t.submitted_at DESC")
    List<TaskRow> selectByOwnerIdWithOwner(Long ownerId);

    /** 按主键查单条，带提交人信息 */
    @Select(JOIN_OWNER + " WHERE t.id = #{id}")
    TaskRow selectByIdWithOwner(Long id);
}
