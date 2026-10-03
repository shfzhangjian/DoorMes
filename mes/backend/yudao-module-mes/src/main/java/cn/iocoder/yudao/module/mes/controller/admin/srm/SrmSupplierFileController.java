package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFilePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFileRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFileSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierFileDO;
import cn.iocoder.yudao.module.mes.service.srm.SrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

@Tag(name = "管理后台 - SRM供应商协议资质")
@RestController
@RequestMapping("/mes/srm/supplier-file")
@Validated
public class SrmSupplierFileController {

    private static final int DEFAULT_WARNING_DAYS = 31;
    private static final String FILE_STATUS_VALID = "VALID";
    private static final String FILE_STATUS_WARNING = "WARNING";
    private static final String FILE_STATUS_EXPIRED = "EXPIRED";

    @Resource
    private SrmService srmService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商协议资质")
    public CommonResult<Long> createSupplierFile(@Valid @RequestBody SrmSupplierFileSaveReqVO reqVO) {
        return success(srmService.createSupplierFile(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商协议资质")
    public CommonResult<Boolean> updateSupplierFile(
            @Validated({Default.class, SrmSupplierFileSaveReqVO.Update.class})
            @RequestBody SrmSupplierFileSaveReqVO reqVO) {
        srmService.updateSupplierFile(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商协议资质")
    public CommonResult<Boolean> deleteSupplierFile(@RequestParam("id") Long id) {
        srmService.deleteSupplierFile(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商协议资质")
    public CommonResult<SrmSupplierFileRespVO> getSupplierFile(@RequestParam("id") Long id) {
        return success(buildResp(srmService.getSupplierFile(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商协议资质分页")
    public CommonResult<PageResult<SrmSupplierFileRespVO>> getSupplierFilePage(@Valid SrmSupplierFilePageReqVO reqVO) {
        PageResult<SrmSupplierFileDO> pageResult = srmService.getSupplierFilePage(reqVO);
        List<SrmSupplierFileRespVO> list = pageResult.getList().stream()
                .map(this::buildResp)
                .toList();
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    private SrmSupplierFileRespVO buildResp(SrmSupplierFileDO entity) {
        SrmSupplierFileRespVO respVO = BeanUtils.toBean(entity, SrmSupplierFileRespVO.class);
        if (respVO == null) {
            return respVO;
        }
        applyPayloadFallback(respVO);
        if (respVO.getExpiryDate() == null) {
            respVO.setFileStatus(FILE_STATUS_VALID);
            return respVO;
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), respVO.getExpiryDate());
        respVO.setDaysLeft(Math.toIntExact(daysLeft));
        Integer warningDays = respVO.getWarningDays() == null
                ? DEFAULT_WARNING_DAYS
                : respVO.getWarningDays();
        if (daysLeft < 0) {
            respVO.setFileStatus(FILE_STATUS_EXPIRED);
        } else if (daysLeft <= warningDays) {
            respVO.setFileStatus(FILE_STATUS_WARNING);
        } else {
            respVO.setFileStatus(FILE_STATUS_VALID);
        }
        return respVO;
    }

    private void applyPayloadFallback(SrmSupplierFileRespVO respVO) {
        JSONObject payload = parsePayload(respVO.getPayloadJson());
        respVO.setProvidedProduct(firstNonBlank(
                respVO.getProvidedProduct(),
                firstPayloadText(payload, "providedProduct", "provided_product", "供应产品", "供应商产品")));
        respVO.setProductModel(firstNonBlank(
                respVO.getProductModel(),
                firstPayloadText(payload, "productModel", "product_model", "产品型号")));
        respVO.setInspectionAgency(firstNonBlank(
                respVO.getInspectionAgency(),
                firstPayloadText(payload, "inspectionAgency", "inspection_agency", "检测机构")));
        respVO.setReportCode(firstNonBlank(
                respVO.getReportCode(),
                firstPayloadText(payload, "reportCode", "report_code", "报告编码", "报告编号")));
        respVO.setStandardCompliant(normalizeStandardCompliant(firstNonBlank(
                respVO.getStandardCompliant(),
                firstPayloadText(payload, "standardCompliant", "standard_compliant", "是否符合标准", "是否符合"))));
        respVO.setExpiryRule(firstNonBlank(
                respVO.getExpiryRule(), firstPayloadText(payload, "expiryRule", "expiry_rule", "有效期规则")));
        respVO.setValidityMonths(firstNonNull(
                respVO.getValidityMonths(), firstPayloadInt(payload, "validityMonths", "validity_months", "有效期月数", "有效期(月)")));
    }

    private JSONObject parsePayload(String payloadJson) {
        if (StrUtil.isBlank(payloadJson)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(payloadJson);
        } catch (RuntimeException ignored) {
            return new JSONObject();
        }
    }

    private String firstNonBlank(String current, String fallback) {
        if (StrUtil.isNotBlank(current)) {
            return current;
        }
        String text = StrUtil.trim(fallback);
        return StrUtil.isBlank(text) ? null : text;
    }

    private String firstPayloadText(JSONObject payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = StrUtil.trim(payload.getStr(key));
            if (StrUtil.isNotBlank(value) && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer firstPayloadInt(JSONObject payload, String... keys) {
        String value = firstPayloadText(payload, keys);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeStandardCompliant(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String upperText = text.toUpperCase(Locale.ROOT);
        if (Set.of("Y", "YES", "TRUE", "1").contains(upperText)
                || Set.of("是", "符合", "合格").contains(text)) {
            return "Y";
        }
        if (Set.of("N", "NO", "FALSE", "0").contains(upperText)
                || Set.of("否", "不符合", "不合格").contains(text)) {
            return "N";
        }
        return text;
    }

    private Integer firstNonNull(Integer current, Integer fallback) {
        return current == null ? fallback : current;
    }

}
