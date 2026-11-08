
package cn.iocoder.yudao.module.system.dal.dataobject.user;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_user_import_task")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserImportTaskDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0待处理 1处理中 2已完成 3失败 */
    private Integer status;

    /** Excel 总记录数 */
    private Integer totalCount;

    /** 已处理记录数 */
    private Integer processedCount;

    /** 成功记录数 */
    private Integer successCount;

    /** 失败记录数 */
    private Integer failCount;

    /** 任务异常信息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime finishTime;
}
