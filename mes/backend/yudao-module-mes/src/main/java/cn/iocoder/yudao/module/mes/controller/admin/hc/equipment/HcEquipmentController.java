package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSimpleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentWorkStateAdjustReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.service.hc.equipment.HcEquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "???? - 设备台账")
@RestController
@RequestMapping("/mes/hc/base/equipment")
@Validated
public class HcEquipmentController {

    @Resource
    private HcEquipmentService hcEquipmentService;

    @PostMapping("/create")
    @Operation(summary = "??设备台账")
    public CommonResult<Long> createHcEquipment(@Valid @RequestBody HcEquipmentSaveReqVO createReqVO) {
        return success(hcEquipmentService.createHcEquipment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??设备台账")
    public CommonResult<Boolean> updateHcEquipment(@Valid @RequestBody HcEquipmentSaveReqVO updateReqVO) {
        hcEquipmentService.updateHcEquipment(updateReqVO);
        return success(true);
    }

    @PutMapping("/adjust-work-state")
    @Operation(summary = "调整设备运行状态")
    public CommonResult<Boolean> adjustWorkState(@Valid @RequestBody HcEquipmentWorkStateAdjustReqVO reqVO) {
        hcEquipmentService.adjustWorkState(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??设备台账")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcEquipment(@RequestParam("id") Long id) {
        hcEquipmentService.deleteHcEquipment(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????设备台账")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcEquipmentList(@RequestParam("ids") List<Long> ids) {
        hcEquipmentService.deleteHcEquipmentListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??设备台账??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcEquipmentRespVO> getHcEquipment(@RequestParam("id") Long id) {
        HcEquipmentDO entity = hcEquipmentService.getHcEquipment(id);
        return success(BeanUtils.toBean(entity, HcEquipmentRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??设备台账????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcEquipmentDetailRespVO> getHcEquipmentDetail(@RequestParam("id") Long id) {
        HcEquipmentDO entity = hcEquipmentService.getHcEquipment(id);
        HcEquipmentDetailRespVO respVO = BeanUtils.toBean(entity, HcEquipmentDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??设备台账????")
    public CommonResult<List<HcEquipmentSimpleRespVO>> getHcEquipmentSimpleList() {
        List<HcEquipmentDO> list = hcEquipmentService.getHcEquipmentSimpleList();
        return success(BeanUtils.toBean(list, HcEquipmentSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??设备台账????")
    public CommonResult<Map<Long, String>> getHcEquipmentSimpleMap() {
        List<HcEquipmentDO> list = hcEquipmentService.getHcEquipmentSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getEquipmentName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??设备台账????")
    public CommonResult<List<HcEquipmentSelectOptionRespVO>> getHcEquipmentSelectOptions() {
        List<HcEquipmentDO> list = hcEquipmentService.getHcEquipmentSimpleList();
        List<HcEquipmentSelectOptionRespVO> result = new ArrayList<>();
        for (HcEquipmentDO item : list) {
            HcEquipmentSelectOptionRespVO option = new HcEquipmentSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getEquipmentName());
            option.setCode(item.getEquipmentCode());
            option.setName(item.getEquipmentName());
            option.setWorkCenterId(item.getWorkCenterId());
            option.setWorkCenterCode(item.getWorkCenterCode());
            option.setWorkCenterName(item.getWorkCenterName());
            option.setApplicablePadType(item.getApplicablePadType());
            option.setApplicablePadTypeName(item.getApplicablePadTypeName());
            option.setStatus(item.getStatus());
            option.setWorkStatus(item.getWorkStatus());
            option.setCurrentOperationCode(item.getCurrentOperationCode());
            option.setCurrentOperationName(item.getCurrentOperationName());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??设备台账??")
    public CommonResult<List<HcEquipmentRespVO>> getHcEquipmentList(@Valid HcEquipmentPageReqVO reqVO) {
        List<HcEquipmentDO> list = hcEquipmentService.getHcEquipmentList(reqVO);
        return success(BeanUtils.toBean(list, HcEquipmentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??设备台账??")
    public CommonResult<PageResult<HcEquipmentRespVO>> getHcEquipmentPage(@Valid HcEquipmentPageReqVO pageReqVO) {
        PageResult<HcEquipmentDO> pageResult = hcEquipmentService.getHcEquipmentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcEquipmentRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??设备台账 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcEquipmentExcel(@Valid HcEquipmentPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcEquipmentDO> list = hcEquipmentService.getHcEquipmentPage(pageReqVO).getList();
        ExcelUtils.write(response, "设备台账.xls", "??", HcEquipmentRespVO.class, BeanUtils.toBean(list, HcEquipmentRespVO.class));
    }

}
