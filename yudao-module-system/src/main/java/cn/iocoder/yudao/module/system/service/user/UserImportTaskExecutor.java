
package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import java.util.List;

@Slf4j
@Component
public class UserImportTaskExecutor {

    private final ThreadPoolTaskExecutor executor;
    private final UserImportTaskService taskService;
    private final AdminUserService userService;

    public UserImportTaskExecutor(
            @Qualifier("userImportExecutor") ThreadPoolTaskExecutor executor,
            UserImportTaskService taskService,
            AdminUserService userService) {
        this.executor = executor;
        this.taskService = taskService;
        this.userService = userService;
    }

    /**
     * 提交后台导入任务
     */

    public void submit(Long taskId,
                       Long tenantId,
                       List<UserImportExcelVO> users) {

        // 1. 创建包含租户上下文的导入任务
        Runnable task = () ->
                TenantUtils.execute(tenantId, () ->
                        executeImport(taskId, users)
                );

        // 2. 包装当前登录用户的 SecurityContext
        Runnable securityTask =
                new DelegatingSecurityContextRunnable(task);

        // 3. 提交到专用线程池
        executor.execute(securityTask);
    }


    /**
     * 后台执行导入
     */

    private void executeImport(Long taskId,
                               List<UserImportExcelVO> users) {

        try {
            // 1. 标记任务开始
            taskService.startTask(taskId);

            // 2. 每批处理 100 条
            final int batchSize = 100;
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < users.size(); i += batchSize) {

                int end = Math.min(i + batchSize, users.size());

                List<UserImportExcelVO> batch =
                        users.subList(i, end);
                if (i == 0) {
                    log.info("第一批第一条用户名：{}", batch.get(0).getUsername());
                    log.info("第一批最后一条用户名：{}", batch.get(batch.size() - 1).getUsername());
                }
                // 3. 调用原版用户导入方法
                // 每批通过 Spring 事务代理独立执行
                UserImportRespVO result =
                        userService.importUserList(batch, false);
                if (!result.getFailureUsernames().isEmpty()) {
                    log.warn("用户导入失败明细：taskId={}, batchStart={}, failures={}",
                            taskId, i, result.getFailureUsernames());
                }
                // 4. 累计成功数量
                successCount +=
                        result.getCreateUsernames().size()
                                + result.getUpdateUsernames().size();

                // 5. 累计失败数量
                failCount +=
                        result.getFailureUsernames().size();

                // 6. 当前批次完成后更新任务进度
                taskService.updateProgress(
                        taskId, successCount, failCount);

                log.info(
                        "导入进度：taskId={}, processed={}/{}",
                        taskId,
                        successCount + failCount,
                        users.size()
                );
            }

            // 7. 所有批次处理完成
            taskService.completeTask(
                    taskId, successCount, failCount);

            log.info(
                    "用户异步导入完成：taskId={}, success={}, fail={}",
                    taskId, successCount, failCount);

        } catch (Exception e) {

            log.error("用户异步导入异常：taskId={}", taskId, e);

            try {
                taskService.failTask(taskId, e.getMessage());
            } catch (Exception updateException) {
                log.error(
                        "更新任务失败状态异常：taskId={}",
                        taskId, updateException);
            }
        }
    }

}
