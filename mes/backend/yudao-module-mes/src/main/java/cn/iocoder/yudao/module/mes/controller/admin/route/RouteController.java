// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.route.RouteController
package cn.iocoder.yudao.module.mes.controller.admin.route;

import cn.iocoder.yudao.module.mes.service.route.RouteService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.module.mes.controller.admin.route.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.module.mes.service.routeprocess.RouteProcessService;

@Tag(name = "管理后台 - MES工艺路线主表")
@RestController
@RequestMapping("/mes/base/route")
@Validated
public class RouteController {

    @Resource
    private RouteService routeService;
    @Resource
    private RouteProcessService routeProcessService;

    @PostMapping("/create")
    @Operation(summary = "创建工艺路线")
    public CommonResult<Long> createRoute(@Valid @RequestBody RouteSaveReqVO createReqVO) {
        return success(routeService.createRoute(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工艺路线")
    public CommonResult<Boolean> updateRoute(@Valid @RequestBody RouteSaveReqVO updateReqVO) {
        routeService.updateRoute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工艺路线")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteRoute(@RequestParam("id") Long id) {
        routeService.deleteRoute(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除工艺路线")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteRouteList(@RequestParam("ids") List<Long> ids) {
        routeService.deleteRouteList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工艺路线详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<RouteRespVO> getRoute(@RequestParam("id") Long id) {
        RouteDO route = routeService.getRoute(id);
        RouteRespVO respVO = BeanUtils.toBean(route, RouteRespVO.class);
        // 填充子表
        respVO.setProcesses(routeProcessService.getProcessListByRouteId(id));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得工艺路线分页")
    public CommonResult<PageResult<RouteRespVO>> getRoutePage(@Valid RoutePageReqVO pageReqVO) {
        PageResult<RouteDO> pageResult = routeService.getRoutePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, RouteRespVO.class));
    }

    // 导出接口略
}
