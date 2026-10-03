package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableBalanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerMarkUsedUpReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumablePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumableSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingProcessConsumableDO;
import cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger.HcToolingConsumableLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 边库耗材领用台账")
@RestController
@RequestMapping("/mes/hc/base/tooling-consumable-ledger")
@Validated
public class HcToolingConsumableLedgerController {

    @Resource
    private HcToolingConsumableLedgerService hcToolingConsumableLedgerService;

    @PostMapping("/create")
    @Operation(summary = "新增边库耗材领用台账")
    public CommonResult<Long> createLedger(@Valid @RequestBody HcToolingConsumableLedgerSaveReqVO reqVO) {
        return success(hcToolingConsumableLedgerService.createLedger(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改边库耗材领用台账")
    public CommonResult<Boolean> updateLedger(@Valid @RequestBody HcToolingConsumableLedgerSaveReqVO reqVO) {
        hcToolingConsumableLedgerService.updateLedger(reqVO);
        return success(true);
    }

    @PutMapping("/usage-status/update")
    @Operation(summary = "修改边库耗材使用状态")
    public CommonResult<Boolean> updateLedgerUsageStatus(@RequestParam("id") Long id,
                                                         @RequestParam("usageStatus") String usageStatus) {
        hcToolingConsumableLedgerService.updateLedgerUsageStatus(id, usageStatus);
        return success(true);
    }

    @PutMapping("/used-up/mark")
    @Operation(summary = "标记边库耗材已用完")
    public CommonResult<Boolean> markLedgerUsedUp(
            @Valid @RequestBody HcToolingConsumableLedgerMarkUsedUpReqVO reqVO) {
        hcToolingConsumableLedgerService.markLedgerUsedUp(reqVO);
        return success(true);
    }

    @PutMapping("/return/register")
    @Operation(summary = "登记边库耗材退库")
    public CommonResult<Boolean> returnLedger(
            @Valid @RequestBody HcToolingConsumableLedgerReturnReqVO reqVO) {
        hcToolingConsumableLedgerService.returnLedger(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除边库耗材领用台账")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteLedger(@RequestParam("id") Long id) {
        hcToolingConsumableLedgerService.deleteLedger(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除边库耗材领用台账")
    public CommonResult<Boolean> deleteLedgerList(@RequestParam("ids") List<Long> ids) {
        hcToolingConsumableLedgerService.deleteLedgerList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得边库耗材领用台账")
    @Parameter(name = "id", required = true)
    public CommonResult<HcToolingConsumableLedgerRespVO> getLedger(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcToolingConsumableLedgerService.getLedger(id),
                HcToolingConsumableLedgerRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "边库耗材领用台账分页")
    public CommonResult<PageResult<HcToolingConsumableLedgerRespVO>> getLedgerPage(
            @Valid HcToolingConsumableLedgerPageReqVO reqVO) {
        PageResult<HcToolingConsumableLedgerDO> page = hcToolingConsumableLedgerService.getLedgerPage(reqVO);
        return success(BeanUtils.toBean(page, HcToolingConsumableLedgerRespVO.class));
    }

    @GetMapping("/balance/page")
    @Operation(summary = "边库耗材余额分页")
    public CommonResult<PageResult<HcToolingConsumableBalanceRespVO>> getBalancePage(
            @Valid HcToolingConsumableLedgerPageReqVO reqVO) {
        return success(hcToolingConsumableLedgerService.getBalancePage(reqVO));
    }

    @PostMapping("/consume/create")
    @Operation(summary = "新增边库耗材消耗明细")
    public CommonResult<Long> createConsume(@Valid @RequestBody HcToolingConsumableConsumeSaveReqVO reqVO) {
        return success(hcToolingConsumableLedgerService.createConsume(reqVO));
    }

    @PutMapping("/consume/update")
    @Operation(summary = "修改边库耗材消耗明细")
    public CommonResult<Boolean> updateConsume(@Valid @RequestBody HcToolingConsumableConsumeSaveReqVO reqVO) {
        hcToolingConsumableLedgerService.updateConsume(reqVO);
        return success(true);
    }

    @DeleteMapping("/consume/delete")
    @Operation(summary = "删除边库耗材消耗明细")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteConsume(@RequestParam("id") Long id) {
        hcToolingConsumableLedgerService.deleteConsume(id);
        return success(true);
    }

    @DeleteMapping("/consume/delete-list")
    @Operation(summary = "批量删除边库耗材消耗明细")
    public CommonResult<Boolean> deleteConsumeList(@RequestParam("ids") List<Long> ids) {
        hcToolingConsumableLedgerService.deleteConsumeList(ids);
        return success(true);
    }

    @GetMapping("/consume/get")
    @Operation(summary = "获得边库耗材消耗明细")
    @Parameter(name = "id", required = true)
    public CommonResult<HcToolingConsumableConsumeRespVO> getConsume(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcToolingConsumableLedgerService.getConsume(id),
                HcToolingConsumableConsumeRespVO.class));
    }

    @GetMapping("/consume/page")
    @Operation(summary = "边库耗材消耗明细分页")
    public CommonResult<PageResult<HcToolingConsumableConsumeRespVO>> getConsumePage(
            @Valid HcToolingConsumableConsumePageReqVO reqVO) {
        return success(hcToolingConsumableLedgerService.getConsumeRecordPage(reqVO));
    }

    @PostMapping("/config/create")
    @Operation(summary = "新增工序耗材字典")
    public CommonResult<Long> createProcessConsumable(@Valid @RequestBody HcToolingProcessConsumableSaveReqVO reqVO) {
        return success(hcToolingConsumableLedgerService.createProcessConsumable(reqVO));
    }

    @PutMapping("/config/update")
    @Operation(summary = "修改工序耗材字典")
    public CommonResult<Boolean> updateProcessConsumable(@Valid @RequestBody HcToolingProcessConsumableSaveReqVO reqVO) {
        hcToolingConsumableLedgerService.updateProcessConsumable(reqVO);
        return success(true);
    }

    @DeleteMapping("/config/delete")
    @Operation(summary = "删除工序耗材字典")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> deleteProcessConsumable(@RequestParam("id") Long id) {
        hcToolingConsumableLedgerService.deleteProcessConsumable(id);
        return success(true);
    }

    @DeleteMapping("/config/delete-list")
    @Operation(summary = "批量删除工序耗材字典")
    public CommonResult<Boolean> deleteProcessConsumableList(@RequestParam("ids") List<Long> ids) {
        hcToolingConsumableLedgerService.deleteProcessConsumableList(ids);
        return success(true);
    }

    @GetMapping("/config/get")
    @Operation(summary = "获得工序耗材字典")
    @Parameter(name = "id", required = true)
    public CommonResult<HcToolingProcessConsumableRespVO> getProcessConsumable(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcToolingConsumableLedgerService.getProcessConsumable(id),
                HcToolingProcessConsumableRespVO.class));
    }

    @GetMapping("/config/page")
    @Operation(summary = "工序耗材字典分页")
    public CommonResult<PageResult<HcToolingProcessConsumableRespVO>> getProcessConsumablePage(
            @Valid HcToolingProcessConsumablePageReqVO reqVO) {
        PageResult<HcToolingProcessConsumableDO> page = hcToolingConsumableLedgerService
                .getProcessConsumablePage(reqVO);
        return success(BeanUtils.toBean(page, HcToolingProcessConsumableRespVO.class));
    }

    @GetMapping("/config/list-by-process")
    @Operation(summary = "按工序获得启用的耗材配置")
    public CommonResult<List<HcToolingProcessConsumableRespVO>> getProcessConsumableListByProcess(
            @RequestParam(value = "processCode", required = false) String processCode) {
        return success(BeanUtils.toBean(hcToolingConsumableLedgerService.getEnabledProcessConsumableList(processCode),
                HcToolingProcessConsumableRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出边库耗材领用台账 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcToolingConsumableLedgerPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "边库耗材领用台账.xlsx", "领用台账",
                HcToolingConsumableLedgerExcelVO.class,
                hcToolingConsumableLedgerService.buildExportList(reqVO));
    }

    @PostMapping("/import")
    @Operation(summary = "导入边库耗材领用台账")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcToolingConsumableLedgerImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "processCode", required = false) String processCode) throws IOException {
        return success(hcToolingConsumableLedgerService.importExcel(file, processCode));
    }
}
