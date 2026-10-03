package cn.iocoder.yudao.module.mes.controller.admin.workshop;

import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workshop.MesWorkshopDO;
import cn.iocoder.yudao.module.mes.service.workshop.MesWorkshopService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

@Tag(name = "管理后台 - MES车间产线定义")
@RestController
@RequestMapping("/mes/base/workshop")
@Validated
public class MesWorkshopController {

    @Resource
    private MesWorkshopService mesWorkshopService;

    @PostMapping("/create")
    @Operation(summary = "创建MES车间产线定义")
    public CommonResult<Long> createMesWorkshop(@Valid @RequestBody MesWorkshopSaveReqVO createReqVO) {
        return success(mesWorkshopService.createMesWorkshop(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新MES车间产线定义")
    public CommonResult<Boolean> updateMesWorkshop(@Valid @RequestBody MesWorkshopSaveReqVO updateReqVO) {
        mesWorkshopService.updateMesWorkshop(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除MES车间产线定义")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteMesWorkshop(@RequestParam("id") Long id) {
        mesWorkshopService.deleteMesWorkshop(id);
        return success(true);
    }


    @GetMapping("/get")
    @Operation(summary = "获得MES车间产线定义")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<MesWorkshopRespVO> getMesWorkshop(@RequestParam("id") Long id) {
        MesWorkshopDO mesWorkshop = mesWorkshopService.getMesWorkshop(id);
        return success(BeanUtils.toBean(mesWorkshop, MesWorkshopRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得MES车间产线定义列表")
    public CommonResult<List<MesWorkshopRespVO>> getMesWorkshopList(@Valid MesWorkshopListReqVO listReqVO) {
        List<MesWorkshopDO> list = mesWorkshopService.getMesWorkshopList(listReqVO);
        return success(BeanUtils.toBean(list, MesWorkshopRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出MES车间产线定义 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMesWorkshopExcel(@Valid MesWorkshopListReqVO listReqVO,
              HttpServletResponse response) throws IOException {
        List<MesWorkshopDO> list = mesWorkshopService.getMesWorkshopList(listReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "MES车间产线定义.xls", "数据", MesWorkshopRespVO.class,
                        BeanUtils.toBean(list, MesWorkshopRespVO.class));
    }

}
