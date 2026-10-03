package cn.iocoder.yudao.module.mes.controller.admin.supplier;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.validation.groups.Default;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.*;
import cn.iocoder.yudao.module.mes.service.supplier.SupplierService;

@Tag(name = "管理后台 - 供应商主数据")
@RestController
@RequestMapping("/mes/supplier")
@Validated
public class MesSupplierController {

    @Resource
    private SupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商主数据")
    public CommonResult<Long> createSupplier(@Valid @RequestBody MesSupplierSaveReqVO createReqVO) {
        return success(supplierService.createSupplier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商主数据")
    public CommonResult<Boolean> updateSupplier(
            @Validated({Default.class, MesSupplierSaveReqVO.Update.class}) @RequestBody MesSupplierSaveReqVO updateReqVO) {
        supplierService.updateSupplier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商主数据")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteSupplier(@RequestParam("id") Long id) {
        supplierService.deleteSupplier(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除供应商主数据")
    public CommonResult<Boolean> deleteSupplierList(@RequestParam("ids") List<Long> ids) {
        supplierService.deleteSupplierListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商主数据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<MesSupplierRespVO> getSupplier(@RequestParam("id") Long id) {
        return success(supplierService.getSupplier(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商主数据分页")
    public CommonResult<PageResult<MesSupplierRespVO>> getSupplierPage(@Valid MesSupplierPageReqVO pageReqVO) {
        return success(supplierService.getSupplierPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出供应商主数据 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplierExcel(@Valid MesSupplierPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesSupplierRespVO> list = supplierService.getSupplierPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "供应商主数据.xls", "数据", MesSupplierRespVO.class, list);
    }

    @GetMapping("/import-template")
    @Operation(summary = "下载供应商名录导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplierImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "供应商名录导入模板.xlsx", "供应商名录",
                MesSupplierImportExcelVO.class, supplierService.buildImportTemplate());
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入供应商名录")
    public CommonResult<MesSupplierImportRespVO> importSupplierExcel(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(supplierService.importSupplierExcel(file));
    }

}
