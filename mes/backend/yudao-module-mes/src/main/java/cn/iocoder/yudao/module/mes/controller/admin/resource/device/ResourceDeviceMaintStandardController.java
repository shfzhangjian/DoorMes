package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardDO;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备保养标准")
@RestController
@RequestMapping("/mes/resource/device/maint-standard")
@Validated
public class ResourceDeviceMaintStandardController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备保养标准")
    public CommonResult<Long> createStandard(@Valid @RequestBody ResourceDeviceMaintStandardSaveReqVO reqVO) {
        return success(resourceDeviceService.createStandard(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备保养标准")
    public CommonResult<Boolean> updateStandard(
            @Validated({Default.class, ResourceDeviceMaintStandardSaveReqVO.Update.class})
            @RequestBody ResourceDeviceMaintStandardSaveReqVO reqVO) {
        resourceDeviceService.updateStandard(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备保养标准")
    public CommonResult<Boolean> deleteStandard(@RequestParam("id") Long id) {
        resourceDeviceService.deleteStandard(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除设备保养标准")
    public CommonResult<Boolean> deleteStandardList(@RequestParam("ids") List<Long> ids) {
        ids.forEach(resourceDeviceService::deleteStandard);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备保养标准")
    public CommonResult<ResourceDeviceMaintStandardRespVO> getStandard(@RequestParam("id") Long id) {
        ResourceDeviceMaintStandardRespVO respVO = BeanUtils.toBean(resourceDeviceService.getStandard(id),
                ResourceDeviceMaintStandardRespVO.class);
        respVO.setItems(BeanUtils.toBean(resourceDeviceService.getStandardItemList(id),
                ResourceDeviceMaintStandardRespVO.StandardItem.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备保养标准分页")
    public CommonResult<PageResult<ResourceDeviceMaintStandardRespVO>> getStandardPage(@Valid ResourceDeviceMaintStandardPageReqVO reqVO) {
        return success(BeanUtils.toBean(resourceDeviceService.getStandardPage(reqVO), ResourceDeviceMaintStandardRespVO.class));
    }

    @GetMapping("/item/list")
    @Operation(summary = "获得设备保养标准项目列表")
    public CommonResult<List<ResourceDeviceMaintStandardRespVO.StandardItem>> getStandardItemList(@RequestParam("standardId") Long standardId) {
        return success(BeanUtils.toBean(resourceDeviceService.getStandardItemList(standardId),
                ResourceDeviceMaintStandardRespVO.StandardItem.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备保养标准 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandardExcel(@Valid ResourceDeviceMaintStandardPageReqVO reqVO,
                                    HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceMaintStandardDO> list = resourceDeviceService.getStandardPage(reqVO).getList();
        ExcelUtils.write(response, "设备保养标准.xls", "数据", ResourceDeviceMaintStandardRespVO.class,
                BeanUtils.toBean(list, ResourceDeviceMaintStandardRespVO.class));
    }

}
