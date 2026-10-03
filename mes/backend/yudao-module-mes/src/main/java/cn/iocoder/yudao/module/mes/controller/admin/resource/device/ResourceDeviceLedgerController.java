package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceLedgerDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
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

@Tag(name = "管理后台 - 设备台账")
@RestController
@RequestMapping("/mes/resource/device/ledger")
@Validated
public class ResourceDeviceLedgerController {

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备台账")
    public CommonResult<Long> createLedger(@Valid @RequestBody ResourceDeviceLedgerSaveReqVO reqVO) {
        return success(resourceDeviceService.createLedger(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备台账")
    public CommonResult<Boolean> updateLedger(
            @Validated({Default.class, ResourceDeviceLedgerSaveReqVO.Update.class})
            @RequestBody ResourceDeviceLedgerSaveReqVO reqVO) {
        resourceDeviceService.updateLedger(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备台账")
    public CommonResult<Boolean> deleteLedger(@RequestParam("id") Long id) {
        resourceDeviceService.deleteLedger(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除设备台账")
    public CommonResult<Boolean> deleteLedgerList(@RequestParam("ids") List<Long> ids) {
        ids.forEach(resourceDeviceService::deleteLedger);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备台账")
    public CommonResult<ResourceDeviceLedgerRespVO> getLedger(@RequestParam("id") Long id) {
        ResourceDeviceLedgerDO ledger = resourceDeviceService.getLedger(id);
        return success(buildLedgerResp(ledger));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备台账分页")
    public CommonResult<PageResult<ResourceDeviceLedgerRespVO>> getLedgerPage(@Valid ResourceDeviceLedgerPageReqVO reqVO) {
        PageResult<ResourceDeviceLedgerDO> pageResult = resourceDeviceService.getLedgerPage(reqVO);
        return success(BeanUtils.toBean(pageResult, ResourceDeviceLedgerRespVO.class));
    }

    @GetMapping("/part/list")
    @Operation(summary = "获得设备关键部件列表")
    public CommonResult<List<ResourceDeviceLedgerRespVO.DevicePart>> getPartList(@RequestParam("deviceId") Long deviceId) {
        return success(BeanUtils.toBean(resourceDeviceService.getPartList(deviceId), ResourceDeviceLedgerRespVO.DevicePart.class));
    }

    @GetMapping("/param/list")
    @Operation(summary = "获得设备技术参数列表")
    public CommonResult<List<ResourceDeviceLedgerRespVO.DeviceParam>> getParamList(@RequestParam("deviceId") Long deviceId) {
        return success(BeanUtils.toBean(resourceDeviceService.getParamList(deviceId), ResourceDeviceLedgerRespVO.DeviceParam.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备台账 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLedgerExcel(@Valid ResourceDeviceLedgerPageReqVO reqVO,
                                  HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceLedgerDO> list = resourceDeviceService.getLedgerPage(reqVO).getList();
        List<ResourceDeviceLedgerRespVO> exportList = BeanUtils.toBean(list, ResourceDeviceLedgerRespVO.class);
        for (int i = 0; i < exportList.size(); i++) {
            exportList.get(i).setRowNo(i + 1);
        }
        ExcelUtils.write(response, "设备台账.xls", "数据", ResourceDeviceLedgerRespVO.class,
                exportList);
    }

    private ResourceDeviceLedgerRespVO buildLedgerResp(ResourceDeviceLedgerDO ledger) {
        ResourceDeviceLedgerRespVO respVO = BeanUtils.toBean(ledger, ResourceDeviceLedgerRespVO.class);
        if (respVO == null || ledger == null) {
            return respVO;
        }
        respVO.setParts(BeanUtils.toBean(resourceDeviceService.getPartList(ledger.getId()), ResourceDeviceLedgerRespVO.DevicePart.class));
        respVO.setParams(BeanUtils.toBean(resourceDeviceService.getParamList(ledger.getId()), ResourceDeviceLedgerRespVO.DeviceParam.class));
        return respVO;
    }

}
