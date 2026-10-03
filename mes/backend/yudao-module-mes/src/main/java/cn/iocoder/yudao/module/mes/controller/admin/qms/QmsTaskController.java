// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.qms.QmsTaskController.java
package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsTaskDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS质量检验任务")
@RestController
@RequestMapping("/mes/qms-task")
@Validated
public class QmsTaskController {

    @Resource
    private QmsTaskService qmsTaskService;

    @PostMapping("/create")
    @Operation(summary = "下发/生成检验任务")
    public CommonResult<Long> createQmsTask(@Valid @RequestBody QmsTaskSaveReqVO createReqVO) {
        return success(qmsTaskService.createQmsTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "记录检验结果")
    public CommonResult<Boolean> updateQmsTask(@Valid @RequestBody QmsTaskSaveReqVO updateReqVO) {
        qmsTaskService.updateQmsTask(updateReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得检验任务分页")
    public CommonResult<PageResult<QmsTaskDO>> getQmsTaskPage(@Valid QmsTaskPageReqVO pageVO) {
        return success(qmsTaskService.getQmsTaskPage(pageVO));
    }
}
