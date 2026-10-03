package cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceRespVO;
import cn.iocoder.yudao.module.mes.service.hc.batchtrace.HcBatchTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HC 批次追溯")
@RestController
@RequestMapping("/mes/hc/plan/batch-trace")
@Validated
public class HcBatchTraceController {

    @Resource
    private HcBatchTraceService batchTraceService;

    @GetMapping("/trace")
    @Operation(summary = "按任意母批、分段、片号或裁切片号查询批次追溯")
    public CommonResult<HcBatchTraceRespVO> trace(@Valid HcBatchTraceQueryReqVO reqVO) {
        return success(batchTraceService.getTrace(reqVO));
    }

}
