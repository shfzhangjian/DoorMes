package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapDO;
import cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap.HcFinishedGlueBoardMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - 成品胶板对照表")
@RestController
@RequestMapping("/mes/hc/base/finished-glue-board-map")
@Validated
public class HcFinishedGlueBoardMapController {

    @Resource
    private HcFinishedGlueBoardMapService finishedGlueBoardMapService;

    @PostMapping("/create")
    @Operation(summary = "创建成品胶板对照")
    public CommonResult<Long> createHcFinishedGlueBoardMap(@Valid @RequestBody HcFinishedGlueBoardMapSaveReqVO createReqVO) {
        return success(finishedGlueBoardMapService.createHcFinishedGlueBoardMap(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成品胶板对照")
    public CommonResult<Boolean> updateHcFinishedGlueBoardMap(@Valid @RequestBody HcFinishedGlueBoardMapSaveReqVO updateReqVO) {
        finishedGlueBoardMapService.updateHcFinishedGlueBoardMap(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成品胶板对照")
    @Parameter(name = "id", description = "主键ID", required = true)
    public CommonResult<Boolean> deleteHcFinishedGlueBoardMap(@RequestParam("id") Long id) {
        finishedGlueBoardMapService.deleteHcFinishedGlueBoardMap(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除成品胶板对照")
    @Parameter(name = "ids", description = "主键ID列表", required = true)
    public CommonResult<Boolean> deleteHcFinishedGlueBoardMapList(@RequestParam("ids") List<Long> ids) {
        finishedGlueBoardMapService.deleteHcFinishedGlueBoardMapListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成品胶板对照")
    public CommonResult<HcFinishedGlueBoardMapRespVO> getHcFinishedGlueBoardMap(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(finishedGlueBoardMapService.getHcFinishedGlueBoardMap(id), HcFinishedGlueBoardMapRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得成品胶板对照详情")
    public CommonResult<HcFinishedGlueBoardMapDetailRespVO> getHcFinishedGlueBoardMapDetail(@RequestParam("id") Long id) {
        HcFinishedGlueBoardMapDO entity = finishedGlueBoardMapService.getHcFinishedGlueBoardMap(id);
        HcFinishedGlueBoardMapDetailRespVO respVO = BeanUtils.toBean(entity, HcFinishedGlueBoardMapDetailRespVO.class);
        respVO.setItems(BeanUtils.toBean(finishedGlueBoardMapService.getItemListByMapId(id), HcFinishedGlueBoardMapItemRespVO.class));
        return success(respVO);
    }

    @GetMapping("/match-items")
    @Operation(summary = "按产品型号和粘胶工序匹配胶板对照明细")
    public CommonResult<List<HcFinishedGlueBoardMapItemRespVO>> getMatchedGlueBoardItems(
            @RequestParam("productModelCode") String productModelCode,
            @RequestParam("glueProcess") String glueProcess) {
        return success(BeanUtils.toBean(finishedGlueBoardMapService.getMatchedItems(productModelCode, glueProcess),
                HcFinishedGlueBoardMapItemRespVO.class));
    }

    @GetMapping("/model-options")
    @Operation(summary = "获得成品胶板对照胶板型号选项")
    public CommonResult<List<HcFinishedGlueBoardMapItemRespVO>> getGlueBoardModelOptions(
            @RequestParam(value = "glueProcess", required = false) String glueProcess,
            @RequestParam(value = "glueBoardMaterialCode", required = false) String glueBoardMaterialCode) {
        return success(BeanUtils.toBean(finishedGlueBoardMapService.getGlueBoardModelItems(glueProcess, glueBoardMaterialCode),
                HcFinishedGlueBoardMapItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成品胶板对照分页")
    public CommonResult<PageResult<HcFinishedGlueBoardMapRespVO>> getHcFinishedGlueBoardMapPage(@Valid HcFinishedGlueBoardMapPageReqVO pageReqVO) {
        PageResult<HcFinishedGlueBoardMapDO> pageResult = finishedGlueBoardMapService.getHcFinishedGlueBoardMapPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcFinishedGlueBoardMapRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出成品胶板对照 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcFinishedGlueBoardMapExcel(@Valid HcFinishedGlueBoardMapPageReqVO pageReqVO,
                                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcFinishedGlueBoardMapDO> list = finishedGlueBoardMapService.getHcFinishedGlueBoardMapPage(pageReqVO).getList();
        ExcelUtils.write(response, "成品胶板对照表.xls", "成品胶板对照表", HcFinishedGlueBoardMapRespVO.class,
                BeanUtils.toBean(list, HcFinishedGlueBoardMapRespVO.class));
    }

}
