package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableDefaultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordLifeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcGrindingProductionRecordLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
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

@Tag(name = "管理后台 - 磨皮生产记录表")
@RestController
@RequestMapping("/mes/hc/plan/production-record/rough-grinding")
@Validated
public class HcGrindingProductionRecordController {

    @Resource
    private HcGrindingProductionRecordLedgerService ledgerService;

    @PostMapping("/create")
    @Operation(summary = "新增磨皮生产记录")
    public CommonResult<Long> create(@Valid @RequestBody HcGrindingProductionRecordSaveReqVO reqVO) {
        return success(ledgerService.create(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改磨皮生产记录")
    public CommonResult<Boolean> update(@Valid @RequestBody HcGrindingProductionRecordSaveReqVO reqVO) {
        ledgerService.update(reqVO); return success(true);
    }

    @PutMapping("/life/update")
    @Operation(summary = "修正磨皮生产记录耗材寿命")
    public CommonResult<Boolean> updateLife(@Valid @RequestBody HcGrindingProductionRecordLifeUpdateReqVO reqVO) {
        ledgerService.updateLife(reqVO); return success(true);
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认磨皮生产记录")
    public CommonResult<Integer> confirm(@Valid @RequestBody HcGrindingProductionRecordConfirmReqVO reqVO) {
        return success(ledgerService.confirm(reqVO));
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        ledgerService.delete(id); return success(true);
    }

    @DeleteMapping("/delete-list")
    public CommonResult<Boolean> deleteList(@RequestParam("ids") List<Long> ids) {
        ledgerService.deleteByIds(ids); return success(true);
    }

    @GetMapping("/get")
    public CommonResult<HcGrindingProductionRecordRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(ledgerService.get(id), HcGrindingProductionRecordRespVO.class));
    }

    @GetMapping("/consumable-default")
    @Operation(summary = "获取磨皮生产记录耗材寿命默认快照")
    public CommonResult<HcGrindingProductionRecordConsumableDefaultRespVO> getConsumableDefault(
            @RequestParam("equipmentId") Long equipmentId,
            @RequestParam("completionTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime completionTime) {
        return success(ledgerService.getConsumableDefault(equipmentId, completionTime));
    }

    @GetMapping("/consumable-sync/latest")
    @Operation(summary = "获取设备最近已确认磨皮生产记录的耗材同步预览")
    public CommonResult<HcGrindingProductionRecordConsumableSyncRespVO> getLatestConfirmedConsumableSync(
            @RequestParam("equipmentId") Long equipmentId) {
        return success(ledgerService.getLatestConfirmedConsumableSync(equipmentId));
    }

    @PostMapping("/consumable-sync/latest")
    @Operation(summary = "按设备同步最近已确认磨皮生产记录至报工耗材")
    public CommonResult<HcGrindingProductionRecordConsumableSyncRespVO> syncLatestConfirmedConsumableState(
            @Valid @RequestBody HcGrindingProductionRecordConsumableSyncReqVO reqVO) {
        return success(ledgerService.syncLatestConfirmedConsumableState(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得磨皮生产记录表分页")
    public CommonResult<PageResult<HcGrindingProductionRecordRespVO>> page(
            @Valid HcGrindingProductionRecordPageReqVO reqVO) {
        PageResult<HcGrindingProductionRecordDO> page = ledgerService.getPage(reqVO);
        return success(BeanUtils.toBean(page, HcGrindingProductionRecordRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出磨皮生产记录表")
    @ApiAccessLog(operateType = EXPORT)
    public void export(@Valid HcGrindingProductionRecordPageReqVO reqVO,
            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "磨皮生产记录表.xlsx", "磨皮生产记录表",
                HcGrindingProductionRecordExcelVO.class, ledgerService.buildExportList(reqVO));
    }

    @PostMapping("/import")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcGrindingProductionRecordImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(ledgerService.importExcel(file));
    }
}
