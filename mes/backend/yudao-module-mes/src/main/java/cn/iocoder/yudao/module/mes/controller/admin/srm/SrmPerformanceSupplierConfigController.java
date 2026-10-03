package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmPerformanceSupplierConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - SRM供应商季度评分配置")
@RestController
@RequestMapping("/mes/srm/performance-supplier-config")
@Validated
public class SrmPerformanceSupplierConfigController {

    @Resource
    private SrmPerformanceSupplierConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商季度评分配置")
    public CommonResult<Long> create(@Valid @RequestBody SrmPerformanceSupplierConfigSaveReqVO reqVO) {
        return success(configService.createConfig(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商季度评分配置")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmPerformanceSupplierConfigSaveReqVO reqVO) {
        configService.updateConfig(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商季度评分配置")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商季度评分配置详情")
    public CommonResult<SrmPerformanceSupplierConfigRespVO> get(@RequestParam("id") Long id) {
        return success(configService.getConfig(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商季度评分配置分页")
    public CommonResult<PageResult<SrmPerformanceSupplierConfigRespVO>> page(
            @Valid SrmPerformanceSupplierConfigPageReqVO reqVO) {
        return success(configService.getConfigPage(reqVO));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的供应商季度评分配置列表")
    public CommonResult<List<SrmPerformanceSupplierConfigRespVO>> enabledList() {
        return success(configService.getEnabledConfigList());
    }

    @GetMapping("/item-config")
    @Operation(summary = "获得供应商模板指标人员与计算配置")
    public CommonResult<SrmPerformanceSupplierConfigRespVO> itemConfig(@RequestParam("configId") Long configId,
                                                                       @RequestParam("templateVersionId") Long templateVersionId) {
        return success(configService.getItemConfig(configId, templateVersionId));
    }

    @PutMapping("/item-config")
    @Operation(summary = "保存供应商模板指标人员与计算配置")
    public CommonResult<Boolean> saveItemConfig(@Valid @RequestBody SrmPerformanceSupplierConfigReqVO reqVO) {
        configService.saveItemConfig(reqVO);
        return success(true);
    }

}
