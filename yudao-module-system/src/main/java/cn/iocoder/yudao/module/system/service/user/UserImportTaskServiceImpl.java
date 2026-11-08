
package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.user.UserImportTaskDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.UserImportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserImportTaskServiceImpl implements UserImportTaskService {

    private final UserImportTaskMapper taskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Integer totalCount) {
        if (totalCount == null || totalCount <= 0) {
            throw new IllegalArgumentException("导入记录数必须大于0");
        }

        UserImportTaskDO task = new UserImportTaskDO();
        task.setStatus(0);
        task.setTotalCount(totalCount);
        task.setProcessedCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);

        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public UserImportTaskDO getTask(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        return taskMapper.selectById(taskId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId) {

        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        // 只有 status = 0 的任务才能改成处理中
        int updatedRows = taskMapper.update(
                null,
                new LambdaUpdateWrapper<UserImportTaskDO>()
                        .eq(UserImportTaskDO::getId, taskId)
                        .eq(UserImportTaskDO::getStatus, 0)
                        .set(UserImportTaskDO::getStatus, 1)
                        .set(UserImportTaskDO::getStartTime,
                                LocalDateTime.now())
        );

        // 没有更新任何记录，说明任务不存在或已经启动
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "任务不存在或已被其他线程启动"
            );
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, Integer successCount, Integer failCount) {

        // 1. 查询任务
        UserImportTaskDO task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }

        // 2. 只有处理中的任务才能完成
        if (!Integer.valueOf(1).equals(task.getStatus())) {
            throw new IllegalStateException("只有处理中的任务才能完成");
        }

        // 3. 校验处理数量
        if (successCount == null || failCount == null
                || successCount < 0 || failCount < 0
                || successCount.longValue() + failCount.longValue()
                != task.getTotalCount().longValue()) {
            throw new IllegalArgumentException("导入数量不正确");
        }

        // 4. 更新任务结果
        task.setStatus(2);
        task.setSuccessCount(successCount);
        task.setFailCount(failCount);
        task.setProcessedCount(successCount + failCount);
        task.setFinishTime(LocalDateTime.now());

        // 5. 保存到数据库
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failTask(Long taskId, String errorMessage) {

        // 1. 查询任务
        UserImportTaskDO task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }

        // 2. 只有待处理或处理中的任务才能失败
        if (!Integer.valueOf(0).equals(task.getStatus())
                && !Integer.valueOf(1).equals(task.getStatus())) {
            throw new IllegalStateException("当前任务不能标记为失败");
        }

        // 3. 更新失败状态
        task.setStatus(3);
        task.setErrorMessage(errorMessage);
        task.setFinishTime(LocalDateTime.now());

        // 4. 保存到数据库
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long taskId, Integer successCount, Integer failCount) {

        // 1. 查询任务
        UserImportTaskDO task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }

        // 2. 只有处理中的任务才允许更新进度
        if (!Integer.valueOf(1).equals(task.getStatus())) {
            throw new IllegalStateException("只有处理中的任务才能更新进度");
        }

        // 3. 校验数量
        if (successCount == null || failCount == null
                || successCount < 0 || failCount < 0) {
            throw new IllegalArgumentException("导入数量不能为负数");
        }

        long processedCount = (long) successCount + failCount;

        if (processedCount > task.getTotalCount()) {
            throw new IllegalArgumentException("已处理数量不能超过总记录数");
        }

        // 4. 更新累计进度
        task.setSuccessCount(successCount);
        task.setFailCount(failCount);
        task.setProcessedCount((int) processedCount);

        // 5. 保存到数据库
        taskMapper.updateById(task);
    }

}
