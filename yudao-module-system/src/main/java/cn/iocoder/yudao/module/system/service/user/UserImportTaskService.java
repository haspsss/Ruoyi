
package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.user.UserImportTaskDO;

public interface UserImportTaskService {

    /**
     * 创建导入任务
     *
     * @param totalCount Excel 记录总数
     * @return 任务 ID
     */
    Long createTask(Integer totalCount);

    /**
     * 查询任务
     *
     * @param taskId 任务 ID
     * @return 任务信息
     */
    UserImportTaskDO getTask(Long taskId);
    /**
     * 开始执行导入任务
     *
     * @param taskId 任务 ID
     */
    void startTask(Long taskId);
    /**
     * 导入任务完成
     *
     * @param taskId 任务 ID
     * @param successCount 成功数量
     * @param failCount 失败数量
     */
    void completeTask(Long taskId, Integer successCount, Integer failCount);

    /**
     * 导入任务异常失败
     *
     * @param taskId 任务 ID
     * @param errorMessage 异常信息
     */
    void failTask(Long taskId, String errorMessage);
    /**
     * 更新导入进度
     *
     * @param taskId 任务 ID
     * @param successCount 累计成功数量
     * @param failCount 累计失败数量
     */
    void updateProgress(Long taskId, Integer successCount, Integer failCount);
}

