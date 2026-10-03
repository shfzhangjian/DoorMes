package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormProcessOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import com.fasterxml.jackson.databind.JsonNode;
import cn.iocoder.yudao.module.mes.service.hc.stationform.HcStationFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - 工位动态表单")
@RestController
@RequestMapping("/mes/hc/base/station-form")
@Validated
public class HcStationFormController {

    @Resource
    private HcStationFormService hcStationFormService;

    @PostMapping("/create")
    @Operation(summary = "新增工位动态表单")
    public CommonResult<Long> createHcStationForm(@Valid @RequestBody HcStationFormSaveReqVO createReqVO) {
        return success(hcStationFormService.createHcStationForm(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改工位动态表单")
    public CommonResult<Boolean> updateHcStationForm(@Valid @RequestBody HcStationFormSaveReqVO updateReqVO) {
        hcStationFormService.updateHcStationForm(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工位动态表单")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<Boolean> deleteHcStationForm(@RequestParam("id") Long id) {
        hcStationFormService.deleteHcStationForm(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除工位动态表单")
    public CommonResult<Boolean> deleteHcStationFormList(@RequestParam("ids") List<Long> ids) {
        hcStationFormService.deleteHcStationFormListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取工位动态表单")
    public CommonResult<HcStationFormRespVO> getHcStationForm(@RequestParam("id") Long id) {
        HcStationFormDO entity = hcStationFormService.getHcStationForm(id);
        return success(BeanUtils.toBean(entity, HcStationFormRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获取工位动态表单详情")
    public CommonResult<HcStationFormDetailRespVO> getHcStationFormDetail(@RequestParam("id") Long id) {
        HcStationFormDO entity = hcStationFormService.getHcStationForm(id);
        HcStationFormDetailRespVO respVO = BeanUtils.toBean(entity, HcStationFormDetailRespVO.class);
        respVO.setPresetHeaderDataJson(
                entity.getPresetHeaderDataJson() == null || entity.getPresetHeaderDataJson().isBlank()
                        ? buildPresetHeaderDataJson(entity)
                        : entity.getPresetHeaderDataJson());
        List<HcStationFormItemRespVO> items = BeanUtils.toBean(hcStationFormService.getHcStationFormItemList(id), HcStationFormItemRespVO.class);
        respVO.setItems(items);
        respVO.setPresetItems(buildPresetItems(entity, items));
        return success(respVO);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取启用的工位动态表单简单列表")
    public CommonResult<List<HcStationFormRespVO>> getHcStationFormSimpleList(
            @RequestParam(value = "processCode", required = false) String processCode) {
        return success(BeanUtils.toBean(hcStationFormService.getHcStationFormSimpleList(processCode), HcStationFormRespVO.class));
    }

    @GetMapping("/resolve-published")
    @Operation(summary = "按正式报工上下文解析动态表单")
    public CommonResult<HcStationFormRespVO> resolvePublishedHcStationForm(
            @RequestParam("processCode") String processCode,
            @RequestParam(value = "modelCode", required = false) String modelCode,
            @RequestParam("formType") String formType,
            @RequestParam(value = "grindingPass", required = false) String grindingPass) {
        return success(BeanUtils.toBean(
                hcStationFormService.resolvePublishedHcStationForm(processCode, modelCode, formType, grindingPass),
                HcStationFormRespVO.class));
    }

    @GetMapping("/process-options")
    @Operation(summary = "获取报工工序选项")
    public CommonResult<List<HcStationFormProcessOptionRespVO>> getReportProcessOptions() {
        return success(hcStationFormService.getReportProcessOptions());
    }

    @GetMapping("/list")
    @Operation(summary = "获取工位动态表单列表")
    public CommonResult<List<HcStationFormRespVO>> getHcStationFormList(@Valid HcStationFormPageReqVO reqVO) {
        return success(BeanUtils.toBean(hcStationFormService.getHcStationFormList(reqVO), HcStationFormRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取工位动态表单分页")
    public CommonResult<PageResult<HcStationFormRespVO>> getHcStationFormPage(@Valid HcStationFormPageReqVO pageReqVO) {
        PageResult<HcStationFormDO> pageResult = hcStationFormService.getHcStationFormPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcStationFormRespVO.class));
    }

    @PostMapping("/excel-import/preview")
    @Operation(summary = "预览导入工位动态表单 Excel")
    public CommonResult<HcStationFormImportRespVO> previewExcelImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "formCode", required = false) String formCode,
            @RequestParam(value = "formName", required = false) String formName,
            @RequestParam(value = "processCode", required = false) String processCode,
            @RequestParam(value = "triggerTimingCode", required = false) String triggerTimingCode,
            @RequestParam(value = "presetTemplate", required = false) String presetTemplate) throws IOException {
        return success(hcStationFormService.previewExcelImport(file, formCode, formName, processCode,
                triggerTimingCode, presetTemplate));
    }

    @PostMapping("/excel-import/confirm")
    @Operation(summary = "确认导入工位动态表单 Excel")
    public CommonResult<HcStationFormImportRespVO> confirmExcelImport(
            @Valid @RequestBody HcStationFormImportConfirmReqVO reqVO) {
        return success(hcStationFormService.confirmExcelImport(reqVO));
    }

    @PostMapping("/excel-preview/export")
    @Operation(summary = "导出工位动态表单预览 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPreviewExcel(@Valid @RequestBody HcStationFormSaveReqVO reqVO,
                                   HttpServletResponse response) throws IOException {
        String filename = reqVO.getFormName() == null || reqVO.getFormName().isBlank()
                ? "动态表单预览.xlsx"
                : reqVO.getFormName() + ".xlsx";
        writeExcelBytes(response, filename, hcStationFormService.exportPreviewExcel(reqVO));
    }

    @PostMapping("/excel-preview/import")
    @Operation(summary = "导入工位动态表单预览 Excel")
    public CommonResult<HcStationFormImportRespVO> importPreviewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("formJson") String formJson) throws IOException {
        return success(hcStationFormService.importPreviewExcel(file, formJson));
    }

    @GetMapping("/config-package/export")
    @Operation(summary = "导出工位动态表单配置包")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfigPackage(@RequestParam("ids") List<Long> ids,
                                    HttpServletResponse response) throws IOException {
        writeJsonBytes(response, "动态表单配置包.json", hcStationFormService.exportConfigPackage(ids));
    }

    @PostMapping("/config-package/import/preview")
    @Operation(summary = "预检导入工位动态表单配置包")
    public CommonResult<HcStationFormConfigPackageRespVO> previewConfigPackageImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(hcStationFormService.previewConfigPackageImport(file));
    }

    @PostMapping("/config-package/import/confirm")
    @Operation(summary = "确认导入工位动态表单配置包")
    public CommonResult<HcStationFormConfigPackageRespVO> confirmConfigPackageImport(
            @Valid @RequestBody HcStationFormConfigPackageImportReqVO reqVO) {
        return success(hcStationFormService.confirmConfigPackageImport(reqVO));
    }

    private void writeExcelBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        String exportFilename = filename == null || filename.isBlank() ? "动态表单预览.xlsx" : filename;
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(exportFilename));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    private void writeJsonBytes(HttpServletResponse response, String filename, byte[] data) throws IOException {
        String exportFilename = filename == null || filename.isBlank() ? "动态表单配置包.json" : filename;
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(exportFilename));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data == null ? new byte[0] : data);
    }

    private String buildPresetHeaderDataJson(HcStationFormDO form) {
        if (form == null || form.getSchemaJson() == null || form.getSchemaJson().isBlank()) {
            return null;
        }
        JsonNode schema = JsonUtils.parseTree(form.getSchemaJson());
        Map<String, Object> header = new LinkedHashMap<>();
        JsonNode fieldsNode = schema.path("headerFields");
        if (fieldsNode.isArray()) {
            fieldsNode.forEach(fieldNode -> {
                if (!fieldNode.isTextual()) {
                    return;
                }
                String key = fieldNode.asText();
                if (key == null || key.isBlank()) {
                    return;
                }
                header.put(key, "");
            });
        }
        if ("WET_FIRST_INSPECTION".equals(form.getFormCode())) {
            header.putIfAbsent("status", "PENDING");
            header.putIfAbsent("result", "");
            header.putIfAbsent("inspectionDesc", "");
            header.putIfAbsent("inspectTime", "");
            header.putIfAbsent("inspector", "");
        }
        if ("WET_SOLID_SEMI".equals(form.getFormCode()) || "WET_OVEN_SEMI".equals(form.getFormCode())) {
            int rowStep = schema.path("rowStep").asInt(2);
            int defaultLength = schema.path("defaultGeneratedLength").asInt(0);
            int defaultRowCount = schema.path("defaultRowCount").asInt(300);
            int generatedLength = defaultLength > 0 ? defaultLength : rowStep * defaultRowCount;
            header.put("generatedLength", generatedLength);
            header.putIfAbsent("finalResult", "");
            header.putIfAbsent("semiWidth", "");
            if ("WET_SOLID_SEMI".equals(form.getFormCode())) {
                header.putIfAbsent("poreDevelopment", "");
            }
        }
        return header.isEmpty() ? null : JsonUtils.toJsonString(header);
    }

    private List<HcStationFormItemRespVO> buildPresetItems(HcStationFormDO form, List<HcStationFormItemRespVO> items) {
        if (form == null) {
            return List.of();
        }
        if (form.getPresetItemsJson() != null && !form.getPresetItemsJson().isBlank()) {
            return parsePresetItems(form.getPresetItemsJson());
        }
        if ("WET_SOLID_SEMI".equals(form.getFormCode()) || "WET_OVEN_SEMI".equals(form.getFormCode())) {
            JsonNode schema = form.getSchemaJson() == null || form.getSchemaJson().isBlank()
                    ? null
                    : JsonUtils.parseTree(form.getSchemaJson());
            int rowStep = schema != null ? schema.path("rowStep").asInt(2) : 2;
            int defaultGeneratedLength = schema != null ? schema.path("defaultGeneratedLength").asInt(0) : 0;
            int defaultRowCount = schema != null ? schema.path("defaultRowCount").asInt(300) : 300;
            int totalLength = defaultGeneratedLength > 0 ? defaultGeneratedLength : rowStep * defaultRowCount;
            int rowCount = Math.max(1, (int) Math.ceil(totalLength / (double) Math.max(1, rowStep)));
            List<HcStationFormItemRespVO> presetItems = new java.util.ArrayList<>();
            for (int index = 0; index < rowCount; index++) {
                HcStationFormItemRespVO item = new HcStationFormItemRespVO();
                item.setItemSeq(index + 1);
                item.setItemCategory("");
                item.setStepNode("");
                item.setItemName("");
                item.setStandardText("");
                item.setValueMode("TEXT");
                item.setDualLabel1("");
                item.setDualLabel2("");
                item.setDefaultResult("OK");
                presetItems.add(item);
            }
            return presetItems;
        }
        return items == null ? List.of() : items;
    }

    private List<HcStationFormItemRespVO> parsePresetItems(String presetItemsJson) {
        try {
            return JsonUtils.parseArray(presetItemsJson, HcStationFormItemRespVO.class);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
