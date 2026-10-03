package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsYieldTargetConfigService;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工序理论产量配置")
@RestController
@RequestMapping("/mes/quality/statistics/yield-target-config")
@Validated
public class QmsYieldTargetConfigController {

    @Resource
    private QmsYieldTargetConfigService qmsYieldTargetConfigService;

    @GetMapping("/page")
    @Operation(summary = "获得工序理论产量配置分页")
    public CommonResult<PageResult<QmsYieldTargetConfigRespVO>> getTargetConfigPage(
            @Valid QmsYieldTargetConfigPageReqVO pageReqVO) {
        return success(qmsYieldTargetConfigService.getTargetConfigPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的工序理论产量配置")
    public CommonResult<List<QmsYieldTargetConfigRespVO>> getSimpleList() {
        return success(qmsYieldTargetConfigService.getSimpleList());
    }

    @GetMapping("/get")
    @Operation(summary = "获得工序理论产量配置详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsYieldTargetConfigRespVO> getTargetConfig(@RequestParam("id") Long id) {
        return success(qmsYieldTargetConfigService.getTargetConfig(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建工序理论产量配置")
    public CommonResult<Long> createTargetConfig(@Valid @RequestBody QmsYieldTargetConfigSaveReqVO createReqVO) {
        return success(qmsYieldTargetConfigService.createTargetConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工序理论产量配置")
    public CommonResult<Boolean> updateTargetConfig(@Valid @RequestBody QmsYieldTargetConfigSaveReqVO updateReqVO) {
        qmsYieldTargetConfigService.updateTargetConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工序理论产量配置")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteTargetConfig(@RequestParam("id") Long id) {
        qmsYieldTargetConfigService.deleteTargetConfig(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出工序理论产量配置")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTargetConfigExcel(@Valid QmsYieldTargetConfigPageReqVO reqVO,
                                        HttpServletResponse response) throws IOException {
        List<QmsYieldTargetConfigExcelVO> list = qmsYieldTargetConfigService.getExportList(reqVO);
        ExcelUtils.write(response, "工序理论产量配置.xls", "数据", QmsYieldTargetConfigExcelVO.class, list);
    }
}
