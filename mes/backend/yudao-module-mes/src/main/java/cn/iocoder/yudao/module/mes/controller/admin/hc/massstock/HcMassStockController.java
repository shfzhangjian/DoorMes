package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockManualSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockShippingDetailRespVO;
import cn.iocoder.yudao.module.mes.service.hc.massstock.HcMassStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 量产备货库存")
@RestController
@RequestMapping("/mes/hc/plan/mass-stock")
@Validated
public class HcMassStockController {

    @Resource
    private HcMassStockService hcMassStockService;

    @GetMapping("/page")
    @Operation(summary = "查询量产备货库存分页")
    public CommonResult<PageResult<HcMassStockRespVO>> getMassStockPage(@Valid HcMassStockPageReqVO pageReqVO) {
        return success(hcMassStockService.getMassStockPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出量产备货库存 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMassStockExcel(@Valid HcMassStockPageReqVO pageReqVO, HttpServletResponse response)
            throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcMassStockRespVO> list = hcMassStockService.getMassStockList(pageReqVO);
        ExcelUtils.write(response, "量产备货库存.xls", "数据", HcMassStockRespVO.class, list);
    }

    @GetMapping("/good-stock/list")
    @Operation(summary = "查询量产备货库存良品库存明细")
    public CommonResult<List<HcMassStockGoodStockRespVO>> getGoodStockList(
            @RequestParam(required = false) String modelCode,
            @RequestParam String motherBatchNo,
            @RequestParam String motherSegmentBatchNo) {
        return success(hcMassStockService.getGoodStockList(modelCode, motherBatchNo, motherSegmentBatchNo));
    }

    @GetMapping("/shipping-detail/list")
    @Operation(summary = "查询量产备货库存需求配货检验明细")
    public CommonResult<List<HcMassStockShippingDetailRespVO>> getShippingDetailList(
            @RequestParam String metric,
            @RequestParam(required = false) String modelCode,
            @RequestParam String motherBatchNo,
            @RequestParam String motherSegmentBatchNo) {
        return success(hcMassStockService.getShippingDetailList(metric, modelCode,
                motherBatchNo, motherSegmentBatchNo));
    }

    @PutMapping("/manual")
    @Operation(summary = "维护量产备货库存人工字段")
    public CommonResult<Boolean> saveManual(@Valid @RequestBody HcMassStockManualSaveReqVO reqVO) {
        return success(hcMassStockService.saveManual(reqVO));
    }

}
