package cn.iocoder.yudao.module.mes.controller.admin.material;

import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.service.material.MesMaterialService;
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

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;


@Tag(name = "管理后台 - MES物料主数据")
@RestController
@RequestMapping("/mes/base/material")
@Validated
public class MesMaterialController {

    @Resource
    private MesMaterialService mesMaterialService;

    @PostMapping("/create")
    @Operation(summary = "创建MES物料主数据")
    public CommonResult<Long> createMesMaterial(@Valid @RequestBody MesMaterialSaveReqVO createReqVO) {
        return success(mesMaterialService.createMesMaterial(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新MES物料主数据")
    public CommonResult<Boolean> updateMesMaterial(@Valid @RequestBody MesMaterialSaveReqVO updateReqVO) {
        mesMaterialService.updateMesMaterial(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除MES物料主数据")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteMesMaterial(@RequestParam("id") Long id) {
        mesMaterialService.deleteMesMaterial(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除MES物料主数据")
    public CommonResult<Boolean> deleteMesMaterialList(@RequestParam("ids") List<Long> ids) {
        mesMaterialService.deleteMesMaterialListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得MES物料主数据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<MesMaterialRespVO> getMesMaterial(@RequestParam("id") Long id) {
        MesMaterialDO mesMaterial = mesMaterialService.getMesMaterial(id);
        return success(BeanUtils.toBean(mesMaterial, MesMaterialRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得MES物料主数据分页")
    public CommonResult<PageResult<MesMaterialRespVO>> getMesMaterialPage(@Valid MesMaterialPageReqVO pageReqVO) {
        PageResult<MesMaterialDO> pageResult = mesMaterialService.getMesMaterialPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesMaterialRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出MES物料主数据 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMesMaterialExcel(@Valid MesMaterialPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesMaterialDO> list = mesMaterialService.getMesMaterialPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "MES物料主数据.xls", "数据", MesMaterialRespVO.class,
                        BeanUtils.toBean(list, MesMaterialRespVO.class));
    }

}
