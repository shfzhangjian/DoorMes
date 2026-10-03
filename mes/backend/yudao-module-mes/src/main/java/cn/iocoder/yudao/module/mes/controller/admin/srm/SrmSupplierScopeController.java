package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSupplierScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM供应商名录范围")
@RestController
@RequestMapping("/mes/srm/supplier-scope")
@Validated
public class SrmSupplierScopeController {

    @Resource
    private SrmSupplierScopeService supplierScopeService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商名录管理范围")
    public CommonResult<Long> createScope(@Valid @RequestBody SrmSupplierScopeSaveReqVO reqVO) {
        return success(supplierScopeService.createScope(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商名录管理范围")
    public CommonResult<Boolean> updateScope(
            @Validated({Default.class, SrmSupplierScopeSaveReqVO.Update.class})
            @RequestBody SrmSupplierScopeSaveReqVO reqVO) {
        supplierScopeService.updateScope(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商名录管理范围")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteScope(@RequestParam("id") Long id) {
        supplierScopeService.deleteScope(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商名录管理范围")
    public CommonResult<SrmSupplierScopeRespVO> getScope(@RequestParam("id") Long id) {
        return success(supplierScopeService.getScope(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商名录管理范围分页")
    public CommonResult<PageResult<SrmSupplierScopeRespVO>> getScopePage(@Valid SrmSupplierScopePageReqVO reqVO) {
        return success(supplierScopeService.getScopePage(reqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得可选供应商名录管理范围")
    public CommonResult<List<SrmSupplierScopeRespVO>> getSimpleScopeList() {
        return success(supplierScopeService.getSimpleScopeList());
    }

    @GetMapping("/mask-field/list")
    @Operation(summary = "获得供应商脱敏字段配置")
    public CommonResult<List<SrmSupplierMaskFieldRespVO>> getMaskFields() {
        return success(supplierScopeService.getMaskFields());
    }

    @PutMapping("/mask-field/update")
    @Operation(summary = "更新供应商脱敏字段配置")
    public CommonResult<Boolean> updateMaskFields(@Valid @RequestBody SrmSupplierMaskFieldSaveReqVO reqVO) {
        supplierScopeService.updateMaskFields(reqVO);
        return success(true);
    }

}
