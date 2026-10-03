package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanWeekSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintPlanDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "管理后台 - 设备年度保养计划")
@RestController
@RequestMapping("/mes/resource/device/maint-plan")
@Validated
public class ResourceDeviceMaintPlanController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备年度保养计划")
    public CommonResult<Long> createMaintPlan(@Valid @RequestBody ResourceDeviceMaintPlanSaveReqVO reqVO) {
        return success(resourceDeviceService.createMaintPlan(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备年度保养计划")
    public CommonResult<Boolean> updateMaintPlan(
            @Validated({Default.class, ResourceDeviceMaintPlanSaveReqVO.Update.class})
            @RequestBody ResourceDeviceMaintPlanSaveReqVO reqVO) {
        resourceDeviceService.updateMaintPlan(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备年度保养计划")
    public CommonResult<Boolean> deleteMaintPlan(@RequestParam("id") Long id) {
        resourceDeviceService.deleteMaintPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备年度保养计划")
    public CommonResult<ResourceDeviceMaintPlanRespVO> getMaintPlan(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintPlan(id), ResourceDeviceMaintPlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备年度保养计划分页")
    public CommonResult<PageResult<ResourceDeviceMaintPlanRespVO>> getMaintPlanPage(
            @Valid ResourceDeviceMaintPlanPageReqVO reqVO) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintPlanPage(reqVO),
                ResourceDeviceMaintPlanRespVO.class));
    }

    @GetMapping("/matrix")
    @Operation(summary = "获得设备年度保养计划矩阵")
    public CommonResult<ResourceDeviceMaintPlanRespVO.Matrix> getMaintPlanMatrix(@RequestParam("year") Integer year,
                                                                                @RequestParam(value = "deviceId", required = false) Long deviceId) {
        return success(resourceDeviceService.getMaintPlanMatrix(year, deviceId));
    }

    @PutMapping("/week")
    @Operation(summary = "保存设备年度保养计划月度周次")
    public CommonResult<Long> saveMaintPlanWeek(@Valid @RequestBody ResourceDeviceMaintPlanWeekSaveReqVO reqVO) {
        return success(resourceDeviceService.saveMaintPlanWeek(reqVO));
    }

    @PostMapping("/copy-year")
    @Operation(summary = "以上一年为模板生成年度保养计划")
    public CommonResult<Integer> copyMaintPlanYear(
            @Validated(ResourceDeviceMaintPlanGenerateReqVO.Copy.class)
            @RequestBody ResourceDeviceMaintPlanGenerateReqVO reqVO) {
        return success(resourceDeviceService.copyMaintPlanYear(reqVO));
    }

    @PostMapping("/generate-orders")
    @Operation(summary = "发布计划并生成保养工单")
    public CommonResult<Integer> generateMaintOrders(
            @Validated(ResourceDeviceMaintPlanGenerateReqVO.Publish.class)
            @RequestBody ResourceDeviceMaintPlanGenerateReqVO reqVO) {
        return success(resourceDeviceService.generateMaintOrdersFromPlan(reqVO.getIds()));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行设备年度保养计划")
    public CommonResult<Long> executeMaintPlan(@Valid @RequestBody ResourceDeviceMaintPlanExecuteReqVO reqVO) {
        return success(resourceDeviceService.executeMaintPlan(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认设备年度保养计划执行")
    public CommonResult<Integer> confirmMaintPlans(@Valid @RequestBody ResourceDeviceMaintPlanConfirmReqVO reqVO) {
        return success(resourceDeviceService.confirmMaintPlans(reqVO));
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入设备年度保养计划 Excel")
    public CommonResult<ResourceDeviceImportRespVO> importMaintPlanExcel(@RequestParam("file") MultipartFile file,
                                                                        @RequestParam(value = "year", required = false) Integer year,
                                                                        @RequestParam(value = "overwrite", required = false) Boolean overwrite)
            throws IOException {
        List<ResourceDeviceMaintPlanImportExcelVO> rows = ExcelUtils.read(file, ResourceDeviceMaintPlanImportExcelVO.class);
        return success(resourceDeviceService.importMaintPlanExcel(rows, year, overwrite));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备年度保养计划 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaintPlanExcel(@Valid ResourceDeviceMaintPlanPageReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceMaintPlanDO> list = resourceDeviceService.getMaintPlanPage(reqVO).getList();
        ExcelUtils.write(response, "设备年度保养计划.xls", "数据", ResourceDeviceMaintPlanRespVO.class,
                BeanUtils.toBean(list, ResourceDeviceMaintPlanRespVO.class));
    }

}
