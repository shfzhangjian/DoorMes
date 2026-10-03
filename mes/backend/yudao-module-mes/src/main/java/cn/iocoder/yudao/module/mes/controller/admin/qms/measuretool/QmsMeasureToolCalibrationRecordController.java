package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationRecordDO;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 量检具校准记录台账")
@RestController
@RequestMapping("/mes/quality/measure-tool/calibration-record")
@Validated
public class QmsMeasureToolCalibrationRecordController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @PostMapping("/create")
    @Operation(summary = "创建量检具校准记录")
    public CommonResult<Long> createCalibrationRecord(@Valid @RequestBody QmsMeasureToolCalibrationRecordSaveReqVO reqVO) {
        return success(measureToolService.createCalibrationRecord(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新量检具校准记录")
    public CommonResult<Boolean> updateCalibrationRecord(
            @Validated({Default.class, QmsMeasureToolCalibrationRecordSaveReqVO.Update.class})
            @RequestBody QmsMeasureToolCalibrationRecordSaveReqVO reqVO) {
        measureToolService.updateCalibrationRecord(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除量检具校准记录")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCalibrationRecord(@RequestParam("id") Long id) {
        measureToolService.deleteCalibrationRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得量检具校准记录")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsMeasureToolCalibrationRecordRespVO> getCalibrationRecord(@RequestParam("id") Long id) {
        QmsMeasureToolCalibrationRecordDO record = measureToolService.getCalibrationRecord(id);
        return success(BeanUtils.toBean(record, QmsMeasureToolCalibrationRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得量检具校准记录分页")
    public CommonResult<PageResult<QmsMeasureToolCalibrationRecordRespVO>> getCalibrationRecordPage(
            @Valid QmsMeasureToolCalibrationRecordPageReqVO reqVO) {
        PageResult<QmsMeasureToolCalibrationRecordDO> pageResult = measureToolService.getCalibrationRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolCalibrationRecordRespVO.class));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "获得量检具校准月度汇总")
    public CommonResult<List<QmsMeasureToolCalibrationMonthlySummaryRespVO>> getCalibrationMonthlySummary(
            @Valid QmsMeasureToolCalibrationMonthlySummaryReqVO reqVO) {
        return success(measureToolService.getCalibrationMonthlySummary(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出量检具校准记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCalibrationRecordExcel(@Valid QmsMeasureToolCalibrationRecordPageReqVO reqVO,
                                             HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMeasureToolCalibrationRecordDO> list = measureToolService.getCalibrationRecordPage(reqVO).getList();
        ExcelUtils.write(response, "量检具校准记录台账.xls", "数据", QmsMeasureToolCalibrationRecordRespVO.class,
                BeanUtils.toBean(list, QmsMeasureToolCalibrationRecordRespVO.class));
    }

}
