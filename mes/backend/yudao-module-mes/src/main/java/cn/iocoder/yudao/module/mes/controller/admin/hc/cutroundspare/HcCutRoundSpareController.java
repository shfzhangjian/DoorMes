package cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.cutroundspare.HcCutRoundSpareService;
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

@Tag(name = "管理后台 - 裁切备件管理")
@RestController
@RequestMapping("/mes/hc/base/cut-round-spare")
@Validated
public class HcCutRoundSpareController {

    @Resource
    private HcCutRoundSpareService hcCutRoundSpareService;

    @GetMapping("/page")
    @Operation(summary = "裁切刀片/毛毡当前状态分页")
    public CommonResult<PageResult<HcCutRoundSpareRespVO>> page(@Valid HcCutRoundSparePageReqVO reqVO) {
        PageResult<HcCutRoundSpareDO> page = hcCutRoundSpareService.getPage(reqVO);
        return success(BeanUtils.toBean(page, HcCutRoundSpareRespVO.class));
    }

    @GetMapping("/record-page")
    @Operation(summary = "裁切刀片/毛毡流水分页")
    public CommonResult<PageResult<HcCutRoundSpareRecordRespVO>> recordPage(@Valid HcCutRoundSpareRecordPageReqVO reqVO) {
        PageResult<HcCutRoundSpareRecordDO> page = hcCutRoundSpareService.getRecordPage(reqVO);
        return success(BeanUtils.toBean(page, HcCutRoundSpareRecordRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得裁切备件状态")
    @Parameter(name = "id", required = true)
    public CommonResult<HcCutRoundSpareRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(hcCutRoundSpareService.get(id), HcCutRoundSpareRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出裁切刀片/毛毡当前状态 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcCutRoundSparePageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "刀片毛毡当前状态.xlsx", "当前状态",
                HcCutRoundSpareImportExcelVO.class,
                hcCutRoundSpareService.buildExportList(reqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存裁切备件状态")
    public CommonResult<Long> save(@Valid @RequestBody HcCutRoundSpareSaveReqVO reqVO) {
        return success(hcCutRoundSpareService.save(reqVO));
    }

    @PostMapping("/rnd-consume")
    @Operation(summary = "登记研发样品裁切备件消耗")
    public CommonResult<String> saveRndConsume(@Valid @RequestBody HcCutRoundSpareRndConsumeSaveReqVO reqVO) {
        return success(hcCutRoundSpareService.saveRndConsume(reqVO));
    }

    @PostMapping("/import")
    @Operation(summary = "导入初始化裁切刀片/毛毡当前状态")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcCutRoundSpareImportRespVO> importState(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmClear", required = false) Boolean confirmClear) throws IOException {
        return success(hcCutRoundSpareService.importStateExcel(file, confirmClear));
    }
}
