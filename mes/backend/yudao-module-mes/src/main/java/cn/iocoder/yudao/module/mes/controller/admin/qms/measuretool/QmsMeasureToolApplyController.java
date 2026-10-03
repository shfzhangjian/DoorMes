package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolApplyDO;
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

@Tag(name = "管理后台 - 量检具新增申请")
@RestController
@RequestMapping("/mes/quality/measure-tool/apply")
@Validated
public class QmsMeasureToolApplyController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @PostMapping("/create")
    @Operation(summary = "创建量检具新增申请")
    public CommonResult<Long> createApply(@Valid @RequestBody QmsMeasureToolApplySaveReqVO reqVO) {
        return success(measureToolService.createApply(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新量检具新增申请")
    public CommonResult<Boolean> updateApply(
            @Validated({Default.class, QmsMeasureToolApplySaveReqVO.Update.class})
            @RequestBody QmsMeasureToolApplySaveReqVO reqVO) {
        measureToolService.updateApply(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除量检具新增申请")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteApply(@RequestParam("id") Long id) {
        measureToolService.deleteApply(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交量检具新增申请")
    public CommonResult<Boolean> submitApply(@Valid @RequestBody QmsMeasureToolApplyActionReqVO reqVO) {
        measureToolService.submitApply(reqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批通过量检具新增申请并写入台账")
    public CommonResult<Long> approveApply(@Valid @RequestBody QmsMeasureToolApplyActionReqVO reqVO) {
        return success(measureToolService.approveApply(reqVO));
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回量检具新增申请")
    public CommonResult<Boolean> rejectApply(@Valid @RequestBody QmsMeasureToolApplyActionReqVO reqVO) {
        measureToolService.rejectApply(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得量检具新增申请")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsMeasureToolApplyRespVO> getApply(@RequestParam("id") Long id) {
        QmsMeasureToolApplyDO apply = measureToolService.getApply(id);
        return success(BeanUtils.toBean(apply, QmsMeasureToolApplyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得量检具新增申请分页")
    public CommonResult<PageResult<QmsMeasureToolApplyRespVO>> getApplyPage(@Valid QmsMeasureToolApplyPageReqVO reqVO) {
        PageResult<QmsMeasureToolApplyDO> pageResult = measureToolService.getApplyPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolApplyRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出量检具新增申请 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportApplyExcel(@Valid QmsMeasureToolApplyPageReqVO reqVO,
                                 HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMeasureToolApplyDO> list = measureToolService.getApplyPage(reqVO).getList();
        ExcelUtils.write(response, "量检具新增申请.xls", "数据", QmsMeasureToolApplyRespVO.class,
                BeanUtils.toBean(list, QmsMeasureToolApplyRespVO.class));
    }

}
