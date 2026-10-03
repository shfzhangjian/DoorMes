package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsDefectCategoryAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 缺陷分类分析")
@RestController
@RequestMapping("/mes/quality/statistics/defect-category-analysis")
@Validated
public class QmsDefectCategoryAnalysisController {

    @Resource
    private QmsDefectCategoryAnalysisService qmsDefectCategoryAnalysisService;

    @GetMapping("/overview")
    @Operation(summary = "获取缺陷分类分析汇总")
    public CommonResult<QmsDefectCategoryAnalysisRespVO> getOverview(@Valid QmsDefectCategoryAnalysisReqVO reqVO) {
        return success(qmsDefectCategoryAnalysisService.getOverview(reqVO));
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获取缺陷分类分析片号明细分页")
    public CommonResult<PageResult<QmsDefectCategoryAnalysisRespVO.DetailRow>> getDetailPage(
            @Valid QmsDefectCategoryAnalysisReqVO reqVO) {
        return success(qmsDefectCategoryAnalysisService.getDetailPage(reqVO));
    }

    @GetMapping("/defect-category-options")
    @Operation(summary = "获取缺陷分类选项")
    public CommonResult<List<String>> getDefectCategoryOptions(@Valid QmsDefectCategoryAnalysisReqVO reqVO) {
        return success(qmsDefectCategoryAnalysisService.getDefectCategoryOptions(reqVO));
    }
}
