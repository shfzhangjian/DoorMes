package cn.iocoder.yudao.module.mes.controller.admin.hc.location;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;
import cn.iocoder.yudao.module.mes.service.hc.location.HcLocationService;
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

@Tag(name = "???? - 库位")
@RestController
@RequestMapping("/mes/hc/base/location")
@Validated
public class HcLocationController {

    @Resource
    private HcLocationService hcLocationService;

    @PostMapping("/create")
    @Operation(summary = "??库位")
    public CommonResult<Long> createHcLocation(@Valid @RequestBody HcLocationSaveReqVO createReqVO) {
        return success(hcLocationService.createHcLocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??库位")
    public CommonResult<Boolean> updateHcLocation(@Valid @RequestBody HcLocationSaveReqVO updateReqVO) {
        hcLocationService.updateHcLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??库位")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcLocation(@RequestParam("id") Long id) {
        hcLocationService.deleteHcLocation(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????库位")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcLocationList(@RequestParam("ids") List<Long> ids) {
        hcLocationService.deleteHcLocationListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??库位??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcLocationRespVO> getHcLocation(@RequestParam("id") Long id) {
        HcLocationDO entity = hcLocationService.getHcLocation(id);
        return success(BeanUtils.toBean(entity, HcLocationRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??库位????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcLocationDetailRespVO> getHcLocationDetail(@RequestParam("id") Long id) {
        HcLocationDO entity = hcLocationService.getHcLocation(id);
        HcLocationDetailRespVO respVO = BeanUtils.toBean(entity, HcLocationDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??库位????")
    public CommonResult<List<HcLocationSimpleRespVO>> getHcLocationSimpleList() {
        List<HcLocationDO> list = hcLocationService.getHcLocationSimpleList();
        return success(BeanUtils.toBean(list, HcLocationSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??库位????")
    public CommonResult<Map<Long, String>> getHcLocationSimpleMap() {
        List<HcLocationDO> list = hcLocationService.getHcLocationSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getLocationName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??库位????")
    public CommonResult<List<HcLocationSelectOptionRespVO>> getHcLocationSelectOptions() {
        List<HcLocationDO> list = hcLocationService.getHcLocationSimpleList();
        List<HcLocationSelectOptionRespVO> result = new ArrayList<>();
        for (HcLocationDO item : list) {
            HcLocationSelectOptionRespVO option = new HcLocationSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getLocationName());
            option.setCode(item.getLocationCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??库位??")
    public CommonResult<List<HcLocationRespVO>> getHcLocationList(@Valid HcLocationPageReqVO reqVO) {
        List<HcLocationDO> list = hcLocationService.getHcLocationList(reqVO);
        return success(BeanUtils.toBean(list, HcLocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??库位??")
    public CommonResult<PageResult<HcLocationRespVO>> getHcLocationPage(@Valid HcLocationPageReqVO pageReqVO) {
        PageResult<HcLocationDO> pageResult = hcLocationService.getHcLocationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcLocationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??库位 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcLocationExcel(@Valid HcLocationPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcLocationDO> list = hcLocationService.getHcLocationPage(pageReqVO).getList();
        ExcelUtils.write(response, "库位.xls", "??", HcLocationRespVO.class, BeanUtils.toBean(list, HcLocationRespVO.class));
    }

}