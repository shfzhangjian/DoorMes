// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.trace.TraceabilityRecordController.java
package cn.iocoder.yudao.module.mes.controller.admin.trace;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.trace.TraceabilityRecordDO;
import cn.iocoder.yudao.module.mes.service.trace.TraceabilityRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 全链路批次追溯")
@RestController
@RequestMapping("/mes/traceability-record")
@Validated
public class TraceabilityRecordController {

    @Resource
    private TraceabilityRecordService traceabilityRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得批次追溯谱系分页 (支持正反向查询)")
    public CommonResult<PageResult<TraceabilityRecordDO>> getTraceRecordPage(@Valid TraceabilityRecordPageReqVO pageVO) {
        return success(traceabilityRecordService.getTraceRecordPage(pageVO));
    }
}
