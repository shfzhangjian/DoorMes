package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintRecordDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备保养执行记录")
@RestController
@RequestMapping("/mes/resource/device/maint-record")
@Validated
public class ResourceDeviceMaintRecordController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @GetMapping("/get")
    @Operation(summary = "获得设备保养执行记录")
    public CommonResult<ResourceDeviceMaintRecordRespVO> getMaintRecord(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintRecord(id), ResourceDeviceMaintRecordRespVO.class));
    }

    @GetMapping("/get-order")
    @Operation(summary = "按执行记录获得原保养工单详情")
    public CommonResult<ResourceDeviceMaintOrderRespVO> getMaintOrderByRecord(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintOrderByRecordId(id), ResourceDeviceMaintOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备保养执行记录分页")
    public CommonResult<PageResult<ResourceDeviceMaintRecordRespVO>> getMaintRecordPage(@Valid ResourceDeviceMaintRecordPageReqVO reqVO) {
        return success(BeanUtils.toBean(resourceDeviceService.getMaintRecordPage(reqVO), ResourceDeviceMaintRecordRespVO.class));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "获得设备保养月度汇总")
    public CommonResult<List<ResourceDeviceMaintRecordRespVO.MonthlySummary>> getMaintRecordMonthlySummary(
            @Valid ResourceDeviceMaintRecordPageReqVO reqVO) {
        return success(resourceDeviceService.getMaintRecordMonthlySummary(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备保养执行记录 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRecordExcel(@Valid ResourceDeviceMaintRecordPageReqVO reqVO,
                                  HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceMaintRecordDO> list = resourceDeviceService.getMaintRecordPage(reqVO).getList();
        ExcelUtils.write(response, "设备保养执行记录.xls", "数据", ResourceDeviceMaintRecordRespVO.class,
                BeanUtils.toBean(list, ResourceDeviceMaintRecordRespVO.class));
    }

}
