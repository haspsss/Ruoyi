
package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserImportTaskRespVO {

    private Long id;

    /** 0待处理 1处理中 2已完成 3失败 */
    private Integer status;

    private Integer totalCount;

    private Integer processedCount;

    private Integer successCount;

    private Integer failCount;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
