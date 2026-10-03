package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardChangeLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsQualityStandardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 检验标准定义")
@RestController
@RequestMapping("/mes/quality/base/standard")
@Validated
public class QmsQualityStandardController {

    @Resource
    private QmsQualityStandardService qmsQualityStandardService;

    private static String getApplyTypeName(String applyType) {
        return switch (applyType.toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "IQC" -> "进料检验标准定义";
            case "FAI" -> "过程首检检验标准定义";
            case "GLUE_BOARD_FAI" -> "胶板检验标准定义";
            case "IPQC" -> "过程检验标准定义";
            case "FQC" -> "成品检验标准定义";
            case "OQC" -> "出货检验标准定义";
            default -> "检验标准定义";
        };
    }

    @PostMapping("/create")
    @Operation(summary = "创建检验标准")
    public CommonResult<Long> createQualityStandard(@Valid @RequestBody QmsQualityStandardSaveReqVO createReqVO) {
        return success(qmsQualityStandardService.createQualityStandard(createReqVO));
    }

    @PostMapping("/{applyType}/create")
    @Operation(summary = "创建指定环节检验标准")
    public CommonResult<Long> createQualityStandardByType(@PathVariable("applyType") String applyType,
                                                          @Valid @RequestBody QmsQualityStandardSaveReqVO createReqVO) {
        return success(qmsQualityStandardService.createQualityStandard(createReqVO, applyType));
    }

    @PutMapping("/update")
    @Operation(summary = "更新检验标准")
    public CommonResult<Boolean> updateQualityStandard(@Valid @RequestBody QmsQualityStandardSaveReqVO updateReqVO) {
        qmsQualityStandardService.updateQualityStandard(updateReqVO);
        return success(true);
    }

    @PutMapping("/{applyType}/update")
    @Operation(summary = "更新指定环节检验标准")
    public CommonResult<Boolean> updateQualityStandardByType(@PathVariable("applyType") String applyType,
                                                             @Valid @RequestBody QmsQualityStandardSaveReqVO updateReqVO) {
        qmsQualityStandardService.updateQualityStandard(updateReqVO, applyType);
        return success(true);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核检验标准")
    public CommonResult<Boolean> auditQualityStandard(@Valid @RequestBody QmsQualityStandardAuditReqVO reqVO) {
        qmsQualityStandardService.auditQualityStandard(reqVO);
        return success(true);
    }

    @PutMapping("/{applyType}/audit")
    @Operation(summary = "审核指定环节检验标准")
    public CommonResult<Boolean> auditQualityStandardByType(@PathVariable("applyType") String applyType,
                                                            @Valid @RequestBody QmsQualityStandardAuditReqVO reqVO) {
        qmsQualityStandardService.auditQualityStandard(reqVO, applyType);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除检验标准")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteQualityStandard(@RequestParam("id") Long id) {
        qmsQualityStandardService.deleteQualityStandard(id);
        return success(true);
    }

    @DeleteMapping("/{applyType}/delete")
    @Operation(summary = "删除指定环节检验标准")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteQualityStandardByType(@PathVariable("applyType") String applyType,
                                                             @RequestParam("id") Long id) {
        qmsQualityStandardService.deleteQualityStandard(id, applyType);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除检验标准")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteQualityStandardList(@RequestParam("ids") List<Long> ids) {
        qmsQualityStandardService.deleteQualityStandardList(ids);
        return success(true);
    }

    @DeleteMapping("/{applyType}/delete-list")
    @Operation(summary = "批量删除指定环节检验标准")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteQualityStandardListByType(@PathVariable("applyType") String applyType,
                                                                 @RequestParam("ids") List<Long> ids) {
        qmsQualityStandardService.deleteQualityStandardList(ids, applyType);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取检验标准详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsQualityStandardRespVO> getQualityStandard(@RequestParam("id") Long id) {
        return success(qmsQualityStandardService.getQualityStandardResp(id));
    }

    @GetMapping("/{applyType}/get")
    @Operation(summary = "获取指定环节检验标准详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsQualityStandardRespVO> getQualityStandardByType(@PathVariable("applyType") String applyType,
                                                                           @RequestParam("id") Long id) {
        return success(qmsQualityStandardService.getQualityStandardResp(id, applyType));
    }

    @GetMapping("/change-log")
    @Operation(summary = "获取检验标准修改内容日志")
    public CommonResult<List<QmsQualityStandardChangeLogRespVO>> getQualityStandardChangeLogs(
            @RequestParam("standardId") Long standardId,
            @RequestParam("applyType") String applyType) {
        return success(qmsQualityStandardService.getQualityStandardChangeLogs(standardId, applyType));
    }

    @GetMapping("/{applyType}/change-log")
    @Operation(summary = "获取指定环节检验标准修改内容日志")
    public CommonResult<List<QmsQualityStandardChangeLogRespVO>> getQualityStandardChangeLogsByType(
            @PathVariable("applyType") String applyType,
            @RequestParam("standardId") Long standardId) {
        return success(qmsQualityStandardService.getQualityStandardChangeLogs(standardId, applyType));
    }

    @GetMapping("/page")
    @Operation(summary = "获取检验标准分页")
    public CommonResult<PageResult<QmsQualityStandardRespVO>> getQualityStandardPage(@Valid QmsQualityStandardPageReqVO pageReqVO) {
        PageResult<QmsQualityStandardDO> pageResult = qmsQualityStandardService.getQualityStandardPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsQualityStandardRespVO.class));
    }

    @GetMapping("/{applyType}/page")
    @Operation(summary = "获取指定环节检验标准分页")
    public CommonResult<PageResult<QmsQualityStandardRespVO>> getQualityStandardPageByType(
            @PathVariable("applyType") String applyType,
            @Valid QmsQualityStandardPageReqVO pageReqVO) {
        PageResult<QmsQualityStandardDO> pageResult = qmsQualityStandardService.getQualityStandardPage(pageReqVO, applyType);
        return success(BeanUtils.toBean(pageResult, QmsQualityStandardRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出检验标准 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportQualityStandardExcel(@Valid QmsQualityStandardPageReqVO pageReqVO,
                                           HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsQualityStandardDO> list = qmsQualityStandardService.getQualityStandardPage(pageReqVO).getList();
        ExcelUtils.write(response, "检验标准定义.xls", "数据", QmsQualityStandardRespVO.class,
                BeanUtils.toBean(list, QmsQualityStandardRespVO.class));
    }

    @GetMapping("/{applyType}/export-excel")
    @Operation(summary = "导出指定环节检验标准 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportQualityStandardExcelByType(@PathVariable("applyType") String applyType,
                                                 @Valid QmsQualityStandardPageReqVO pageReqVO,
                                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsQualityStandardDO> list = qmsQualityStandardService.getQualityStandardPage(pageReqVO, applyType).getList();
        ExcelUtils.write(response, getApplyTypeName(applyType) + ".xls", "数据", QmsQualityStandardRespVO.class,
                BeanUtils.toBean(list, QmsQualityStandardRespVO.class));
    }
}
