package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRuntimeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.guideclothrecord.HcGuideClothRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;

@Tag(name = "管理后台 - 导布更换记录")
@RestController
@RequestMapping("/mes/hc/base/guide-cloth-record")
@Validated
public class HcGuideClothRecordController {

    @Resource
    private HcGuideClothRecordService hcGuideClothRecordService;

    @PostMapping("/create")
    @Operation(summary = "新增导布更换记录")
    public CommonResult<Long> create(@Valid @RequestBody HcGuideClothRecordSaveReqVO reqVO) {
        return success(hcGuideClothRecordService.create(reqVO));
    }

    @PostMapping("/rnd-consume")
    @Operation(summary = "登记湿法导布研发样品消耗")
    public CommonResult<String> saveRndConsume(@Valid @RequestBody HcGuideClothRndConsumeSaveReqVO reqVO) {
        return success(hcGuideClothRecordService.saveRndConsume(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改导布更换记录")
    public CommonResult<Boolean> update(@Valid @RequestBody HcGuideClothRecordSaveReqVO reqVO) {
        hcGuideClothRecordService.update(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除导布更换记录")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        hcGuideClothRecordService.delete(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除导布更换记录")
    public CommonResult<Boolean> deleteList(@RequestParam("ids") List<Long> ids) {
        hcGuideClothRecordService.deleteByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得导布更换记录")
    public CommonResult<HcGuideClothRecordRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcGuideClothRecordService.get(id), HcGuideClothRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "导布更换记录分页")
    public CommonResult<PageResult<HcGuideClothRecordRespVO>> page(@Valid HcGuideClothRecordPageReqVO reqVO) {
        PageResult<HcGuideClothRecordDO> page = hcGuideClothRecordService.getPage(reqVO);
        return success(BeanUtils.toBean(page, HcGuideClothRecordRespVO.class));
    }

    @GetMapping("/runtime")
    @Operation(summary = "按母料型号和机台获得导布运行时信息")
    public CommonResult<HcGuideClothRuntimeRespVO> runtime(@RequestParam("motherModelCode") String motherModelCode,
                                                           @RequestParam(value = "equipmentId", required = false) Long equipmentId) {
        return success(hcGuideClothRecordService.getRuntimeByMotherModelCode(motherModelCode, equipmentId));
    }

    @GetMapping("/rnd-runtime")
    @Operation(summary = "获得研发消耗登记的当前湿法导布状态")
    public CommonResult<HcGuideClothRuntimeRespVO> getRndRuntime(@RequestParam("guideClothRecordId") Long guideClothRecordId) {
        return success(hcGuideClothRecordService.getRndRuntime(guideClothRecordId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导布更换记录导出")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcGuideClothRecordPageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "导布更换记录.xlsx", "导布更换记录", HcGuideClothRecordImportExcelVO.class,
                hcGuideClothRecordService.buildExportList(reqVO));
    }

    @PostMapping("/import")
    @Operation(summary = "导入初始化导布更换记录")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcGuideClothRecordImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmClear", required = false) Boolean confirmClear) throws IOException {
        return success(hcGuideClothRecordService.importExcel(file, confirmClear));
    }
}
