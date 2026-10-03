package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.service.hc.equipmentconsumable.HcEquipmentConsumableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备耗材寿命管理")
@RestController
@RequestMapping("/mes/hc/base/equipment-consumable")
@Validated
public class HcEquipmentConsumableController {

    @Resource
    private HcEquipmentConsumableService hcEquipmentConsumableService;

    @GetMapping("/state-page")
    @Operation(summary = "设备耗材当前状态分页")
    public CommonResult<PageResult<HcEquipmentConsumableStateRespVO>> statePage(@Valid HcEquipmentConsumableStatePageReqVO reqVO) {
        PageResult<HcEquipmentConsumableStateDO> page = hcEquipmentConsumableService.getStatePage(reqVO);
        return success(BeanUtils.toBean(page, HcEquipmentConsumableStateRespVO.class));
    }

    @GetMapping("/event-page")
    @Operation(summary = "设备耗材更换/调整流水分页")
    public CommonResult<PageResult<HcEquipmentConsumableEventRespVO>> eventPage(@Valid HcEquipmentConsumableEventPageReqVO reqVO) {
        PageResult<HcEquipmentConsumableEventDO> page = hcEquipmentConsumableService.getEventPage(reqVO);
        return success(BeanUtils.toBean(page, HcEquipmentConsumableEventRespVO.class));
    }

    @GetMapping("/get-state")
    @Operation(summary = "获得设备耗材当前状态")
    @Parameter(name = "id", required = true)
    public CommonResult<HcEquipmentConsumableStateRespVO> getState(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcEquipmentConsumableService.getState(id), HcEquipmentConsumableStateRespVO.class));
    }

    @GetMapping("/state-export-excel")
    @Operation(summary = "导出砂纸/导布当前寿命状态 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStateExcel(@Valid HcEquipmentConsumableStatePageReqVO reqVO,
                                 HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "砂纸导布当前寿命状态.xlsx", "当前寿命状态",
                HcEquipmentConsumableStateImportExcelVO.class,
                hcEquipmentConsumableService.buildStateExportList(reqVO));
    }

    @PostMapping("/adjust")
    @Operation(summary = "更换或调整设备耗材状态")
    public CommonResult<Long> adjust(@Valid @RequestBody HcEquipmentConsumableAdjustReqVO reqVO) {
        return success(hcEquipmentConsumableService.adjust(reqVO));
    }

    @PostMapping("/state-import")
    @Operation(summary = "导入初始化砂纸/导布当前寿命状态")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcEquipmentConsumableStateImportRespVO> importState(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmClear", required = false) Boolean confirmClear) throws IOException {
        return success(hcEquipmentConsumableService.importStateExcel(file, confirmClear));
    }
}
