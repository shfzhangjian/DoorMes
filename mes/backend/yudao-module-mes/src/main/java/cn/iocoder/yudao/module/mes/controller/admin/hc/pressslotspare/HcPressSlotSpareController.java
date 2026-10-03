package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.pressslotspare.HcPressSlotSpareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 压槽备件管理")
@RestController
@RequestMapping("/mes/hc/base/press-slot-spare")
@Validated
public class HcPressSlotSpareController {

    @Resource
    private HcPressSlotSpareService hcPressSlotSpareService;

    @GetMapping("/page")
    @Operation(summary = "压槽备件当前状态分页")
    public CommonResult<PageResult<HcPressSlotSpareRespVO>> page(@Valid HcPressSlotSparePageReqVO reqVO) {
        PageResult<HcPressSlotSpareDO> page = hcPressSlotSpareService.getPage(reqVO);
        return success(BeanUtils.toBean(page, HcPressSlotSpareRespVO.class));
    }

    @GetMapping("/record-page")
    @Operation(summary = "压槽备件流水分页")
    public CommonResult<PageResult<HcPressSlotSpareRecordRespVO>> recordPage(@Valid HcPressSlotSpareRecordPageReqVO reqVO) {
        PageResult<HcPressSlotSpareRecordDO> page = hcPressSlotSpareService.getRecordPage(reqVO);
        return success(BeanUtils.toBean(page, HcPressSlotSpareRecordRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得压槽备件状态")
    @Parameter(name = "id", required = true)
    public CommonResult<HcPressSlotSpareRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcPressSlotSpareService.get(id), HcPressSlotSpareRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出压槽辊/轴承当前状态 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcPressSlotSparePageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "压槽辊轴承当前状态.xlsx", "当前状态",
                HcPressSlotSpareImportExcelVO.class,
                hcPressSlotSpareService.buildExportList(reqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存压槽备件状态")
    public CommonResult<Long> save(@Valid @RequestBody HcPressSlotSpareSaveReqVO reqVO) {
        return success(hcPressSlotSpareService.save(reqVO));
    }

    @PostMapping("/rnd-consume")
    @Operation(summary = "登记研发样品压槽备件消耗")
    public CommonResult<String> saveRndConsume(@Valid @RequestBody HcPressSlotSpareRndConsumeSaveReqVO reqVO) {
        return success(hcPressSlotSpareService.saveRndConsume(reqVO));
    }

    @PostMapping("/import")
    @Operation(summary = "导入初始化压槽辊/轴承当前状态")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcPressSlotSpareImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmClear", required = false) Boolean confirmClear) throws IOException {
        return success(hcPressSlotSpareService.importExcel(file, confirmClear));
    }
}
