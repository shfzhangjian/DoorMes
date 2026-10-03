package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateVarRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateDO;
import cn.iocoder.yudao.module.mes.service.hc.customerprinttemplate.HcCustomerPrintTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 客户打印模板")
@RestController
@RequestMapping("/mes/hc/execution/customer-print-template")
@Validated
public class HcCustomerPrintTemplateController {

    @Resource
    private HcCustomerPrintTemplateService hcCustomerPrintTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建客户打印模板")
    public CommonResult<Long> createHcCustomerPrintTemplate(
            @Valid @RequestBody HcCustomerPrintTemplateSaveReqVO createReqVO) {
        return success(hcCustomerPrintTemplateService.createHcCustomerPrintTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户打印模板")
    public CommonResult<Boolean> updateHcCustomerPrintTemplate(
            @Valid @RequestBody HcCustomerPrintTemplateSaveReqVO updateReqVO) {
        hcCustomerPrintTemplateService.updateHcCustomerPrintTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户打印模板")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcCustomerPrintTemplate(@RequestParam("id") Long id) {
        hcCustomerPrintTemplateService.deleteHcCustomerPrintTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户打印模板")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcCustomerPrintTemplateRespVO> getHcCustomerPrintTemplate(@RequestParam("id") Long id) {
        HcCustomerPrintTemplateDO entity = hcCustomerPrintTemplateService.getHcCustomerPrintTemplate(id);
        return success(BeanUtils.toBean(entity, HcCustomerPrintTemplateRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得客户打印模板详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcCustomerPrintTemplateRespVO> getHcCustomerPrintTemplateDetail(@RequestParam("id") Long id) {
        return success(hcCustomerPrintTemplateService.getHcCustomerPrintTemplateDetail(id));
    }

    @GetMapping("/public/download")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "公开下载客户打印模板")
    @Parameter(name = "id", description = "模板编号", required = true)
    public void downloadPublicTemplate(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        HcCustomerPrintTemplateDO entity = hcCustomerPrintTemplateService.getHcCustomerPrintTemplate(id);
        if (entity == null) {
            writeNotFound(response, "客户打印模板不存在");
            return;
        }
        String templateFormat = StrUtil.blankToDefault(entity.getTemplateFormat(), "ZPL").trim().toUpperCase();
        if ("NLBL".equals(templateFormat) && StrUtil.isNotBlank(entity.getTemplateContent())) {
            try {
                writeDownload(response, resolveFilename(entity, "nlbl"), Base64.getDecoder().decode(entity.getTemplateContent()));
                return;
            } catch (IllegalArgumentException ignored) {
                if (StrUtil.isBlank(entity.getFileUrl())) {
                    writeNotFound(response, "客户 NLBL 模板文件内容无效");
                    return;
                }
            }
        }
        if (StrUtil.isBlank(entity.getTemplateContent()) && StrUtil.isNotBlank(entity.getFileUrl())) {
            response.sendRedirect(normalizeRedirectUrl(entity.getFileUrl()));
            return;
        }
        if (StrUtil.isBlank(entity.getTemplateContent())) {
            writeNotFound(response, "客户打印模板内容为空");
            return;
        }
        writeDownload(response, resolveFilename(entity, "zpl"), entity.getTemplateContent().getBytes(StandardCharsets.UTF_8));
    }

    private String resolveFilename(HcCustomerPrintTemplateDO entity, String defaultExt) {
        return StrUtil.blankToDefault(entity.getFileName(),
                StrUtil.blankToDefault(entity.getTemplateCode(), "customer-print-template") + "." + defaultExt);
    }

    private void writeDownload(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户打印模板分页")
    public CommonResult<PageResult<HcCustomerPrintTemplateRespVO>> getHcCustomerPrintTemplatePage(
            @Valid HcCustomerPrintTemplatePageReqVO pageReqVO) {
        PageResult<HcCustomerPrintTemplateDO> pageResult = hcCustomerPrintTemplateService
                .getHcCustomerPrintTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcCustomerPrintTemplateRespVO.class));
    }

    @GetMapping("/vars")
    @Operation(summary = "获得客户打印模板变量")
    @Parameter(name = "templateId", description = "模板编号", required = true)
    public CommonResult<List<HcCustomerPrintTemplateVarRespVO>> getVars(@RequestParam("templateId") Long templateId) {
        return success(hcCustomerPrintTemplateService.getVarsByTemplateId(templateId));
    }

    @GetMapping("/active-list")
    @Operation(summary = "获得启用的客户打印模板")
    public CommonResult<List<HcCustomerPrintTemplateRespVO>> getActiveTemplates(
            @RequestParam("templateType") String templateType,
            @RequestParam(value = "customerCode", required = false) String customerCode,
            @RequestParam(value = "customerName", required = false) String customerName) {
        return success(hcCustomerPrintTemplateService.getActiveTemplates(templateType, customerCode, customerName));
    }

    @GetMapping("/field-options")
    @Operation(summary = "获得客户打印模板字段选项")
    public CommonResult<List<HcCustomerPrintTemplateFieldOptionRespVO>> getFieldOptions(
            @RequestParam(value = "scope", required = false) String scope) {
        return success(hcCustomerPrintTemplateService.getFieldOptions(scope));
    }

    @PostMapping("/parse-variables")
    @Operation(summary = "解析客户打印模板变量")
    public CommonResult<List<HcCustomerPrintTemplateVarRespVO>> parseVariables(
            @Valid @RequestBody HcCustomerPrintTemplateParseReqVO reqVO) {
        return success(hcCustomerPrintTemplateService.parseVariables(reqVO));
    }

    private void writeNotFound(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private String normalizeRedirectUrl(String url) {
        String normalized = url.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/")) {
            return normalized;
        }
        return "/" + normalized;
    }

}
