package cn.iocoder.yudao.module.mes.controller.admin.hc.route;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;
import cn.iocoder.yudao.module.mes.service.hc.route.HcRouteService;
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

@Tag(name = "???? - 工艺路线")
@RestController
@RequestMapping("/mes/hc/base/route")
@Validated
public class HcRouteController {

    @Resource
    private HcRouteService hcRouteService;

    @PostMapping("/create")
    @Operation(summary = "??工艺路线")
    public CommonResult<Long> createHcRoute(@Valid @RequestBody HcRouteSaveReqVO createReqVO) {
        return success(hcRouteService.createHcRoute(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??工艺路线")
    public CommonResult<Boolean> updateHcRoute(@Valid @RequestBody HcRouteSaveReqVO updateReqVO) {
        hcRouteService.updateHcRoute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??工艺路线")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcRoute(@RequestParam("id") Long id) {
        hcRouteService.deleteHcRoute(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????工艺路线")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcRouteList(@RequestParam("ids") List<Long> ids) {
        hcRouteService.deleteHcRouteListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??工艺路线??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcRouteRespVO> getHcRoute(@RequestParam("id") Long id) {
        HcRouteDO entity = hcRouteService.getHcRoute(id);
        return success(BeanUtils.toBean(entity, HcRouteRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??工艺路线????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcRouteDetailRespVO> getHcRouteDetail(@RequestParam("id") Long id) {
        HcRouteDO entity = hcRouteService.getHcRoute(id);
        HcRouteDetailRespVO respVO = BeanUtils.toBean(entity, HcRouteDetailRespVO.class);
        respVO.setRouteOperations(hcRouteService.getHcRouteOperationListByParentId(id));
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??工艺路线????")
    public CommonResult<List<HcRouteSimpleRespVO>> getHcRouteSimpleList() {
        List<HcRouteDO> list = hcRouteService.getHcRouteSimpleList();
        return success(BeanUtils.toBean(list, HcRouteSimpleRespVO.class));
    }

    @GetMapping("/simple-list-by-material-id")
    @Operation(summary = "按物料查询工艺路线精简列表")
    @Parameter(name = "materialId", description = "物料ID", required = true)
    public CommonResult<List<HcRouteSimpleRespVO>> getHcRouteSimpleListByMaterialId(@RequestParam("materialId") Long materialId) {
        List<HcRouteDO> list = hcRouteService.getHcRouteSimpleListByMaterialId(materialId);
        return success(BeanUtils.toBean(list, HcRouteSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??工艺路线????")
    public CommonResult<Map<Long, String>> getHcRouteSimpleMap() {
        List<HcRouteDO> list = hcRouteService.getHcRouteSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getRouteName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??工艺路线????")
    public CommonResult<List<HcRouteSelectOptionRespVO>> getHcRouteSelectOptions() {
        List<HcRouteDO> list = hcRouteService.getHcRouteSimpleList();
        List<HcRouteSelectOptionRespVO> result = new ArrayList<>();
        for (HcRouteDO item : list) {
            HcRouteSelectOptionRespVO option = new HcRouteSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getRouteName());
            option.setCode(item.getRouteCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/select-options-by-material-id")
    @Operation(summary = "按物料查询工艺路线下拉选项")
    @Parameter(name = "materialId", description = "物料ID", required = true)
    public CommonResult<List<HcRouteSelectOptionRespVO>> getHcRouteSelectOptionsByMaterialId(@RequestParam("materialId") Long materialId) {
        List<HcRouteDO> list = hcRouteService.getHcRouteSimpleListByMaterialId(materialId);
        List<HcRouteSelectOptionRespVO> result = new ArrayList<>();
        for (HcRouteDO item : list) {
            HcRouteSelectOptionRespVO option = new HcRouteSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getRouteName());
            option.setCode(item.getRouteCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??工艺路线??")
    public CommonResult<List<HcRouteRespVO>> getHcRouteList(@Valid HcRoutePageReqVO reqVO) {
        List<HcRouteDO> list = hcRouteService.getHcRouteList(reqVO);
        return success(BeanUtils.toBean(list, HcRouteRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??工艺路线??")
    public CommonResult<PageResult<HcRouteRespVO>> getHcRoutePage(@Valid HcRoutePageReqVO pageReqVO) {
        PageResult<HcRouteDO> pageResult = hcRouteService.getHcRoutePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcRouteRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??工艺路线 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcRouteExcel(@Valid HcRoutePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcRouteDO> list = hcRouteService.getHcRoutePage(pageReqVO).getList();
        ExcelUtils.write(response, "工艺路线.xls", "??", HcRouteRespVO.class, BeanUtils.toBean(list, HcRouteRespVO.class));
    }

    @GetMapping("/mes_md_route_operation/list-by-parent-id")
    @Operation(summary = "??路线工序??")
    @Parameter(name = "parentId", description = "??ID", required = true)
    public CommonResult<List<HcRouteOperationDO>> getHcRouteOperationListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcRouteService.getHcRouteOperationListByParentId(parentId));
    }

}
