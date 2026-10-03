package cn.iocoder.yudao.module.mes.controller.admin.hc.processform;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutExcelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordSignerConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.processform.HcProcessFormExcelService;
import cn.iocoder.yudao.module.mes.service.hc.processform.HcProcessFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - HC各工序表单填写记录")
@RestController
@RequestMapping("/mes/hc/base/process-form-record")
@Validated
public class HcProcessFormRecordController {

    @Resource
    private HcProcessFormService processFormService;
    @Resource
    private HcProcessFormExcelService processFormExcelService;

    @PostMapping("/create")
    @Operation(summary = "新增各工序表单填写记录")
    public CommonResult<Long> create(@Valid @RequestBody HcProcessFormRecordSaveReqVO reqVO) {
        return success(processFormService.createRecord(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改各工序表单填写记录")
    public CommonResult<Boolean> update(@Valid @RequestBody HcProcessFormRecordSaveReqVO reqVO) {
        processFormService.updateRecord(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除各工序表单填写记录")
    @Parameter(name = "id", required = true)
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        processFormService.deleteRecord(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交各工序表单填写记录")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        processFormService.submitRecord(id);
        return success(true);
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认各工序表单填写记录")
    public CommonResult<Boolean> confirm(@RequestParam("id") Long id) {
        processFormService.confirmRecord(id);
        return success(true);
    }

    @PostMapping("/confirm-by-signer")
    @Operation(summary = "按身份认证人员确认各工序表单填写记录")
    public CommonResult<Boolean> confirmBySigner(@Valid @RequestBody HcProcessFormRecordSignerConfirmReqVO reqVO) {
        processFormService.confirmRecordBySigner(reqVO.getId(), reqVO.getConfirmUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得各工序表单填写记录")
    public CommonResult<HcProcessFormRecordRespVO> get(@RequestParam("id") Long id) {
        HcProcessFormRecordDO entity = processFormService.getRecord(id);
        return success(BeanUtils.toBean(entity, HcProcessFormRecordRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得各工序表单填写记录详情")
    public CommonResult<HcProcessFormRecordDetailRespVO> getDetail(@RequestParam("id") Long id) {
        return success(processFormService.getRecordDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "各工序表单填写记录分页")
    public CommonResult<PageResult<HcProcessFormRecordRespVO>> page(@Valid HcProcessFormRecordPageReqVO reqVO) {
        PageResult<HcProcessFormRecordDO> page = processFormService.getRecordPage(reqVO);
        return success(BeanUtils.toBean(page, HcProcessFormRecordRespVO.class));
    }

    @PostMapping("/export-layout")
    @Operation(summary = "按弹窗布局导出各工序表单 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLayout(@Valid @RequestBody HcProcessFormLayoutExcelReqVO reqVO,
                             HttpServletResponse response) throws IOException {
        writeExcelBytes(response, reqVO.getFileName(), processFormExcelService.buildLayoutWorkbook(reqVO));
    }

    @PostMapping("/import-layout")
    @Operation(summary = "按弹窗布局导入各工序表单 Excel")
    public CommonResult<HcProcessFormLayoutImportRespVO> importLayout(@RequestParam("file") MultipartFile file,
                                                                      @RequestParam("layoutJson") String layoutJson) throws IOException {
        return success(processFormExcelService.importLayout(file, layoutJson));
    }

    private void writeExcelBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        String exportFilename = filename == null || filename.isBlank() ? "工序表单.xlsx" : filename;
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(exportFilename));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.getOutputStream().write(data);
    }
}
