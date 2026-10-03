package cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.RenameReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceRespVO;
import cn.iocoder.yudao.module.mes.service.hc.sliceadjust.HcSliceAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 片号调账")
@RestController
@RequestMapping("/mes/hc/execution/slice-adjust")
@Validated
public class HcSliceAdjustController {

    @Resource
    private HcSliceAdjustService sliceAdjustService;

    @GetMapping("/candidate-list")
    @Operation(summary = "查询可调账片号候选")
    public CommonResult<List<CandidateRespVO>> getCandidateList(@Valid CandidateQueryReqVO reqVO) {
        return success(sliceAdjustService.getCandidateList(reqVO));
    }

    @GetMapping("/trace-list")
    @Operation(summary = "查询两个片号的关联单据时间线")
    public CommonResult<List<TraceRespVO>> getTraceList(@Valid TraceQueryReqVO reqVO) {
        return success(sliceAdjustService.getTraceList(reqVO));
    }

    @GetMapping("/record-list")
    @Operation(summary = "查询片号调账审计记录")
    public CommonResult<List<AuditRecordRespVO>> getRecordList(@Valid AuditQueryReqVO reqVO) {
        return success(sliceAdjustService.getAuditRecordList(reqVO));
    }

    @PostMapping("/swap")
    @Operation(summary = "调换两个片号的全流程关联片号")
    public CommonResult<SwapRespVO> swapSliceNo(@Valid @RequestBody SwapReqVO reqVO) {
        return success(sliceAdjustService.swapSliceNo(reqVO));
    }

    @PostMapping("/rename")
    @Operation(summary = "直接修改单片片号的全流程关联片号")
    public CommonResult<SwapRespVO> renameSliceNo(@Valid @RequestBody RenameReqVO reqVO) {
        return success(sliceAdjustService.renameSliceNo(reqVO));
    }
}
