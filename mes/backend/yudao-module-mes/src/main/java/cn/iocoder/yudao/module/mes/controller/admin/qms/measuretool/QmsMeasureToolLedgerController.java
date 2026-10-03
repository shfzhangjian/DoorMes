package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainCalibrationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainMsaReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolStatusRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolStatusRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolStatusRecordDO;
import cn.iocoder.yudao.module.mes.service.qms.measuretool.QmsMeasureToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 量检具台账")
@RestController
@RequestMapping("/mes/quality/measure-tool/ledger")
@Validated
public class QmsMeasureToolLedgerController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @PostMapping("/create")
    @Operation(summary = "创建量检具台账")
    public CommonResult<Long> createLedger(@Valid @RequestBody QmsMeasureToolLedgerSaveReqVO reqVO) {
        return success(measureToolService.createLedger(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新量检具台账")
    public CommonResult<Boolean> updateLedger(
            @Validated({Default.class, QmsMeasureToolLedgerSaveReqVO.Update.class})
            @RequestBody QmsMeasureToolLedgerSaveReqVO reqVO) {
        measureToolService.updateLedger(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "调整量检具状态")
    public CommonResult<Boolean> updateLedgerStatus(@Valid @RequestBody QmsMeasureToolLedgerStatusUpdateReqVO reqVO) {
        measureToolService.updateLedgerStatus(reqVO);
        return success(true);
    }

    @GetMapping("/status-record-page")
    @Operation(summary = "获得量检具状态调整记录分页")
    public CommonResult<PageResult<QmsMeasureToolStatusRecordRespVO>> getStatusRecordPage(
            @Valid QmsMeasureToolStatusRecordPageReqVO reqVO) {
        PageResult<QmsMeasureToolStatusRecordDO> pageResult = measureToolService.getStatusRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolStatusRecordRespVO.class));
    }

    @PutMapping("/maintain-calibration")
    @Operation(summary = "维护量检具最新校准结果")
    public CommonResult<Boolean> maintainCalibration(@Valid @RequestBody QmsMeasureToolMaintainCalibrationReqVO reqVO) {
        measureToolService.maintainCalibration(reqVO);
        return success(true);
    }

    @PutMapping("/maintain-msa")
    @Operation(summary = "维护量检具最新MSA分析结果")
    public CommonResult<Boolean> maintainMsa(@Valid @RequestBody QmsMeasureToolMaintainMsaReqVO reqVO) {
        measureToolService.maintainMsa(reqVO);
        return success(true);
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入量检具台账 Excel")
    public CommonResult<Integer> importLedgerExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return success(measureToolService.importLedgerExcel(file));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除量检具台账")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteLedger(@RequestParam("id") Long id) {
        measureToolService.deleteLedger(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得量检具台账")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsMeasureToolLedgerRespVO> getLedger(@RequestParam("id") Long id) {
        QmsMeasureToolLedgerDO ledger = measureToolService.getLedger(id);
        return success(BeanUtils.toBean(ledger, QmsMeasureToolLedgerRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得量检具台账分页")
    public CommonResult<PageResult<QmsMeasureToolLedgerRespVO>> getLedgerPage(@Valid QmsMeasureToolLedgerPageReqVO reqVO) {
        PageResult<QmsMeasureToolLedgerDO> pageResult = measureToolService.getLedgerPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolLedgerRespVO.class));
    }

    @GetMapping("/select-options")
    @Operation(summary = "获取量检具台账可输入下拉候选")
    public CommonResult<QmsMeasureToolLedgerSelectOptionRespVO> getLedgerSelectOptions() {
        return success(measureToolService.getLedgerSelectOptions());
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出量检具台账 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLedgerExcel(@Valid QmsMeasureToolLedgerPageReqVO reqVO,
                                  HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMeasureToolLedgerDO> list = measureToolService.getLedgerPage(reqVO).getList();
        ExcelUtils.write(response, "量检具台账.xls", "数据", QmsMeasureToolLedgerRespVO.class,
                BeanUtils.toBean(list, QmsMeasureToolLedgerRespVO.class));
    }

}
