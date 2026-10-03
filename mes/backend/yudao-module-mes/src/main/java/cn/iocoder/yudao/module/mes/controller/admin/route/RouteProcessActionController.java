// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.route.RouteProcessActionController.java
package cn.iocoder.yudao.module.mes.controller.admin.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteProcessActionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import cn.iocoder.yudao.module.mes.service.route.RouteProcessActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工序SOP动作定义")
@RestController
@RequestMapping("/mes/route-process-action")
@Validated
public class RouteProcessActionController {

    @Resource
    private RouteProcessActionService routeProcessActionService;

    @PostMapping("/create")
    @Operation(summary = "创建工序SOP动作定义")
    public CommonResult<Long> createRouteProcessAction(@Valid @RequestBody RouteProcessActionSaveReqVO createReqVO) {
        return success(routeProcessActionService.createRouteProcessAction(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工序SOP动作定义")
    public CommonResult<Boolean> updateRouteProcessAction(@Valid @RequestBody RouteProcessActionSaveReqVO updateReqVO) {
        routeProcessActionService.updateRouteProcessAction(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工序SOP动作定义")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteRouteProcessAction(@RequestParam("id") Long id) {
        routeProcessActionService.deleteRouteProcessAction(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工序SOP动作定义")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<RouteProcessActionDO> getRouteProcessAction(@RequestParam("id") Long id) {
        return success(routeProcessActionService.getRouteProcessAction(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工序SOP动作定义分页")
    public CommonResult<PageResult<RouteProcessActionDO>> getRouteProcessActionPage(@Valid RouteProcessActionPageReqVO pageVO) {
        return success(routeProcessActionService.getRouteProcessActionPage(pageVO));
    }
}
