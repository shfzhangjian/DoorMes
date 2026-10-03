package cn.iocoder.yudao.module.mes.controller.admin.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.service.workorder.MesWorkOrderSubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 派工细单")
@RestController
@RequestMapping("/mes/work-order-sub")
@Validated
public class MesWorkOrderSubController {

    @Resource
    private MesWorkOrderSubService mesWorkOrderSubService;

    @PostMapping("/create")
    @Operation(summary = "创建派工细单", description = "通常由主工单拆分生成，也可手动创建")
    public CommonResult<Long> createWorkOrderSub(@Valid @RequestBody MesWorkOrderSubSaveReqVO createReqVO) {
        return success(mesWorkOrderSubService.createWorkOrderSub(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改派工细单")
    public CommonResult<Boolean> updateWorkOrderSub(@Valid @RequestBody MesWorkOrderSubSaveReqVO updateReqVO) {
        mesWorkOrderSubService.updateWorkOrderSub(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除派工细单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteWorkOrderSub(@RequestParam("id") Long id) {
        mesWorkOrderSubService.deleteWorkOrderSub(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得派工细单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<MesWorkOrderSubRespVO> getWorkOrderSub(@RequestParam("id") Long id) {
        MesWorkOrderSubDO subDO = mesWorkOrderSubService.getWorkOrderSub(id);
        return success(BeanUtils.toBean(subDO, MesWorkOrderSubRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得派工细单分页")
    public CommonResult<PageResult<MesWorkOrderSubRespVO>> getWorkOrderSubPage(@Valid MesWorkOrderSubPageReqVO pageVO) {
        PageResult<MesWorkOrderSubDO> pageResult = mesWorkOrderSubService.getWorkOrderSubPage(pageVO);
        return success(BeanUtils.toBean(pageResult, MesWorkOrderSubRespVO.class));
    }

}
