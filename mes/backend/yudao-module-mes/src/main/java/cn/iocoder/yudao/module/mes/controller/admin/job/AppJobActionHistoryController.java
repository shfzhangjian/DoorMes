// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.job.AppJobActionHistoryController.java
package cn.iocoder.yudao.module.mes.controller.admin.job;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistoryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistorySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.job.JobActionHistoryDO;
import cn.iocoder.yudao.module.mes.service.job.JobActionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "移动端 - 动态SOP作业执行打卡")
@RestController
@RequestMapping("/mes/app/job-action-history") // 🎯 路由规范：/app-api/mes/app/...
@Validated
public class AppJobActionHistoryController {

    @Resource
    private JobActionHistoryService jobActionHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建作业执行历史(现场打卡)")
    public CommonResult<Long> createJobActionHistory(@Valid @RequestBody JobActionHistorySaveReqVO createReqVO) {
        return success(jobActionHistoryService.createJobActionHistory(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得作业执行历史单条记录")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<JobActionHistoryDO> getJobActionHistory(@RequestParam("id") Long id) {
        // 实际开发中 DO 建议转 RespVO 输出，为演示主流程紧凑度，暂直接返回
        return success(jobActionHistoryService.getJobActionHistory(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得作业执行历史分页")
    public CommonResult<PageResult<JobActionHistoryDO>> getJobActionHistoryPage(@Valid JobActionHistoryPageReqVO pageVO) {
        return success(jobActionHistoryService.getJobActionHistoryPage(pageVO));
    }
}
