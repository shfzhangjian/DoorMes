package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncStatusRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsYieldAnalysisV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 良品率分析（新版）")
@RestController
@RequestMapping("/mes/quality/statistics/yield-analysis-v2")
@Validated
public class QmsYieldAnalysisV2Controller {

    @Resource
    private QmsYieldAnalysisV2Service qmsYieldAnalysisV2Service;

    @GetMapping("/page")
    @Operation(summary = "获取按实际报工过滤的母卷批次良品统计分页")
    public CommonResult<PageResult<QmsMotherRollGoodStatisticsRespVO>> getPage(
            @Valid QmsYieldAnalysisV2PageReqVO reqVO) {
        return success(qmsYieldAnalysisV2Service.getPage(reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获取按实际报工过滤的全部母卷批次良品统计")
    public CommonResult<List<QmsMotherRollGoodStatisticsRespVO>> getList(
            @Valid QmsYieldAnalysisV2PageReqVO reqVO) {
        return success(qmsYieldAnalysisV2Service.getList(reqVO));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取良品率分析（新版）图表汇总")
    public CommonResult<QmsYieldAnalysisRespVO> getOverview(@Valid QmsYieldAnalysisV2PageReqVO reqVO) {
        return success(qmsYieldAnalysisV2Service.getOverview(reqVO));
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获取良品率分析（新版）实际报工钻取明细")
    public CommonResult<PageResult<QmsYieldAnalysisDetailRespVO>> getDetailPage(
            @Valid QmsYieldAnalysisV2PageReqVO reqVO) {
        return success(qmsYieldAnalysisV2Service.getDetailPage(reqVO));
    }

    @GetMapping("/target-model-options")
    @Operation(summary = "获取良品率分析（新版）型号下拉")
    public CommonResult<List<String>> getTargetModelOptions() {
        return success(qmsYieldAnalysisV2Service.getTargetModelOptions());
    }

    @GetMapping("/sync-status")
    @Operation(summary = "获取当前良品率分析日结数据时间")
    public CommonResult<QmsYieldAnalysisV2SyncStatusRespVO> getSyncStatus() {
        return success(qmsYieldAnalysisV2Service.getSyncStatus());
    }

    @PostMapping("/sync")
    @Operation(summary = "差异增量同步良品率分析最新业务状态")
    public CommonResult<QmsYieldAnalysisV2SyncRespVO> syncUnsettledHistory() {
        return success(qmsYieldAnalysisV2Service.syncUnsettledHistory());
    }

}
