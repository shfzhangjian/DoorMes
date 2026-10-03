package cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReleaseReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.intermediatestockledger.HcIntermediateStockLedgerDO;
import cn.iocoder.yudao.module.mes.service.hc.intermediatestockledger.HcIntermediateStockLedgerService;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - 工序中间品库台账")
@RestController
@RequestMapping("/mes/hc/execution/intermediate-stock-ledger")
@Validated
public class HcIntermediateStockLedgerController {

    @Resource
    private HcIntermediateStockLedgerService intermediateStockLedgerService;

    @Resource
    private HcPlanOrderService hcPlanOrderService;

    @GetMapping("/page")
    @Operation(summary = "查询工序中间品库台账分页")
    public CommonResult<PageResult<HcIntermediateStockLedgerRespVO>> getIntermediateStockLedgerPage(
            @Valid HcIntermediateStockLedgerPageReqVO pageReqVO) {
        PageResult<HcIntermediateStockLedgerDO> pageResult =
                intermediateStockLedgerService.getIntermediateStockLedgerPage(pageReqVO);
        return success(buildRespPage(pageResult));
    }

    @GetMapping("/aggregate-page")
    @Operation(summary = "查询工序中间品库台账母卷聚合分页")
    public CommonResult<PageResult<HcIntermediateStockLedgerRespVO>> getIntermediateStockLedgerAggregatePage(
            @Valid HcIntermediateStockLedgerPageReqVO pageReqVO) {
        PageResult<HcIntermediateStockLedgerDO> pageResult =
                intermediateStockLedgerService.getIntermediateStockLedgerAggregatePage(pageReqVO);
        return success(buildRespPage(pageResult));
    }

    @GetMapping("/history-import-template")
    @Operation(summary = "导出工序中间品库历史导入整理模板")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHistoryImportTemplate(@Valid HcIntermediateStockLedgerPageReqVO pageReqVO,
                                            HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcIntermediateStockHistoryImportExcelVO> list =
                intermediateStockLedgerService.buildHistoryImportTemplate(pageReqVO);
        ExcelUtils.write(response, "工序中间品历史导入模板.xlsx", "历史中间品库存",
                HcIntermediateStockHistoryImportExcelVO.class, list);
    }

    @PostMapping("/history-import")
    @Operation(summary = "导入工序中间品库历史台账")
    public CommonResult<HcIntermediateStockHistoryImportRespVO> importHistoryStock(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(intermediateStockLedgerService.importHistoryStock(file));
    }

    @PutMapping("/inventory-lock/release")
    @Operation(summary = "工序中间品库台账释放计划锁定余量")
    public CommonResult<Boolean> releaseInventoryLock(@Valid @RequestBody HcPlanOrderInventoryLockReleaseReqVO reqVO) {
        hcPlanOrderService.releaseInventoryLock(reqVO);
        return success(true);
    }

    private PageResult<HcIntermediateStockLedgerRespVO> buildRespPage(PageResult<HcIntermediateStockLedgerDO> pageResult) {
        List<HcIntermediateStockLedgerRespVO> list = pageResult.getList().stream()
                .map(row -> {
                    HcIntermediateStockLedgerRespVO respVO = BeanUtils.toBean(row, HcIntermediateStockLedgerRespVO.class);
                    respVO.setTxnDetails(BeanUtils.toBean(row.getTxnDetails(),
                            HcIntermediateStockLedgerRespVO.TxnDetailRespVO.class));
                    return respVO;
                })
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

}
