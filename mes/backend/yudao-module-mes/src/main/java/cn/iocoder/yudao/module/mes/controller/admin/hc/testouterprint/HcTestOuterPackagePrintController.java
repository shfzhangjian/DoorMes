package cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintPayloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitPieceListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitSegmentPageReqVO;
import cn.iocoder.yudao.module.mes.service.hc.testouterprint.HcTestOuterPackagePrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 测试外包装打印")
@RestController
@RequestMapping("/mes/hc/barcode-management/test-outer-package-print")
@Validated
public class HcTestOuterPackagePrintController {

    @Resource
    private HcTestOuterPackagePrintService testOuterPackagePrintService;

    @GetMapping("/wait-segment/page")
    @Operation(summary = "获得合格待包装段分页")
    public CommonResult<PageResult<TestOuterSegmentRespVO>> getWaitSegmentPage(
            @Valid TestOuterWaitSegmentPageReqVO reqVO) {
        return success(testOuterPackagePrintService.getWaitSegmentPage(reqVO));
    }

    @GetMapping("/wait-segment/piece-list")
    @Operation(summary = "获得合格待包装段片号列表")
    public CommonResult<List<TestOuterPieceRespVO>> getWaitSegmentPieceList(
            @Valid TestOuterWaitPieceListReqVO reqVO) {
        return success(testOuterPackagePrintService.getWaitSegmentPieceList(reqVO));
    }

    @GetMapping("/customer-product/page")
    @Operation(summary = "获得客户产品分页")
    public CommonResult<PageResult<TestOuterCustomerProductRespVO>> getCustomerProductPage(
            @Valid TestOuterCustomerProductPageReqVO reqVO) {
        return success(testOuterPackagePrintService.getCustomerProductPage(reqVO));
    }

    @GetMapping("/template-list")
    @Operation(summary = "获得客户产品可选打印模板")
    public CommonResult<List<TestOuterPrintDesignRespVO>> getTemplateList(
            @RequestParam("customerInfoId") Long customerInfoId,
            @RequestParam("productItemId") Long productItemId,
            @RequestParam("labelKind") String labelKind) {
        return success(testOuterPackagePrintService.getTemplateList(customerInfoId, productItemId, labelKind));
    }

    @GetMapping("/print-payload")
    @Operation(summary = "构建外包装测试打印负载")
    public CommonResult<TestOuterPrintPayloadRespVO> buildPrintPayload(
            @RequestParam("customerInfoId") Long customerInfoId,
            @RequestParam("productItemId") Long productItemId,
            @RequestParam(value = "designId", required = false) Long designId,
            @RequestParam("labelKind") String labelKind,
            @RequestParam("sliceBatchNos") String sliceBatchNos) {
        return success(testOuterPackagePrintService.buildPrintPayload(customerInfoId, productItemId, designId,
                labelKind,
                sliceBatchNos));
    }

}
