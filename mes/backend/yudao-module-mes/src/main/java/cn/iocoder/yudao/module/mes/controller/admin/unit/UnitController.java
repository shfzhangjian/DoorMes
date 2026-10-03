package cn.iocoder.yudao.module.mes.controller.admin.unit;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.mes.controller.admin.unit.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import cn.iocoder.yudao.module.mes.service.unit.UnitService;

@Tag(name = "管理后台 - MES 计量单位")
@RestController
@RequestMapping("/mes/base/unit")
@Validated
public class UnitController {

    @Resource
    private UnitService unitService;

    @PostMapping("/create")
    @Operation(summary = "创建MES计量单位")
    public CommonResult<Long> createUnit(@Valid @RequestBody UnitSaveReqVO createReqVO) {
        return success(unitService.createUnit(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新MES计量单位")
    public CommonResult<Boolean> updateUnit(@Valid @RequestBody UnitSaveReqVO updateReqVO) {
        unitService.updateUnit(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除MES计量单位")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteUnit(@RequestParam("id") Long id) {
        unitService.deleteUnit(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除MES计量单位")
    public CommonResult<Boolean> deleteUnitList(@RequestParam("ids") List<Long> ids) {
        unitService.deleteUnitListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得MES计量单位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<UnitRespVO> getUnit(@RequestParam("id") Long id) {
        UnitDO unit = unitService.getUnit(id);
        return success(BeanUtils.toBean(unit, UnitRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得MES计量单位分页")
    public CommonResult<PageResult<UnitRespVO>> getUnitPage(@Valid UnitPageReqVO pageReqVO) {
        PageResult<UnitDO> pageResult = unitService.getUnitPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, UnitRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出MES计量单位 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUnitExcel(@Valid UnitPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<UnitDO> list = unitService.getUnitPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "MES计量单位.xls", "数据", UnitRespVO.class,
                        BeanUtils.toBean(list, UnitRespVO.class));
    }

}
