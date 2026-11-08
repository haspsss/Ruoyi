
package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.service.user.UserImportTaskExecutor;
import cn.iocoder.yudao.module.system.service.user.UserImportTaskService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportTaskRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserImportTaskDO;
import org.springframework.security.access.AccessDeniedException;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/system/user")
public class UserImportTaskController {

    @Resource
    private UserImportTaskService taskService;

    @Resource
    private UserImportTaskExecutor taskExecutor;

    /**
     * 异步导入用户
     */
    @PostMapping("/import-async")
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<Long> importAsync(
            @RequestParam("file") MultipartFile file) throws Exception {

        // 1. 解析 Excel
        List<UserImportExcelVO> users =
                ExcelUtils.read(file, UserImportExcelVO.class);

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Excel 不能为空");
        }

        // 2. 创建数据库任务记录
        Long taskId = taskService.createTask(users.size());

        // 3. 获取当前租户
        Long tenantId = TenantContextHolder.getTenantId();

        // 4. 提交后台线程池
        try {
            taskExecutor.submit(taskId, tenantId, users);
        } catch (RejectedExecutionException e) {
            taskService.failTask(taskId, "线程池已满，任务提交失败");
            throw new IllegalStateException("导入任务繁忙，请稍后重试", e);
        }

        // 5. 立即返回任务 ID，不等待导入完成
        return success(taskId);
    }

    /**
     * 查询当前登录用户创建的导入任务
     */
    @GetMapping("/import-task/{taskId}")
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportTaskRespVO> getImportTask(
            @PathVariable("taskId") Long taskId,
            HttpServletRequest request) {
        // 第一条：打印请求头中的租户 ID（新添加）
        System.out.println("【导入任务查询】请求头 tenant-id = "
                + request.getHeader("tenant-id"));
        System.out.println("【导入任务查询】请求头 visit-tenant-id = "
                + request.getHeader("visit-tenant-id"));
        System.out.println("【导入任务查询】当前租户ID = "
                + TenantContextHolder.getTenantId());
        // 1. 查询任务（租户插件负责租户数据隔离）
        UserImportTaskDO task = taskService.getTask(taskId);

        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }

        // 2. 检查任务是否属于当前登录用户
        Long loginUserId = getLoginUserId();
        if (loginUserId == null
                || !String.valueOf(loginUserId).equals(task.getCreator())) {
            throw new AccessDeniedException("无权查看该导入任务");
        }

        // 3. 转换为响应 VO
        UserImportTaskRespVO vo = new UserImportTaskRespVO();
        vo.setId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(task.getTotalCount());
        vo.setProcessedCount(task.getProcessedCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartTime(task.getStartTime());
        vo.setFinishTime(task.getFinishTime());

        return success(vo);
    }

}
