package cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo.HcLotInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo.HcLotInstanceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.service.hc.lotinstance.HcLotInstanceService;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 批次实例台账")
@RestController
@RequestMapping("/mes/hc/base/lot-instance")
@Validated
public class HcLotInstanceController {

    @Resource
    private HcLotInstanceService hcLotInstanceService;

    @Resource
    private HcLotRuleService hcLotRuleService;

    @GetMapping("/page")
    @Operation(summary = "获得真实批次实例台账分页")
    public CommonResult<PageResult<HcLotInstanceRespVO>> getHcLotInstancePage(
            @Valid HcLotInstancePageReqVO pageReqVO) {
        PageResult<HcLotInstanceDO> pageResult = hcLotInstanceService.getLotInstancePage(pageReqVO);
        List<HcLotInstanceRespVO> list = pageResult.getList().stream().map(this::toRespVO).toList();
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得真实批次实例详情")
    @Parameter(name = "id", description = "批次实例ID", required = true)
    public CommonResult<HcLotInstanceRespVO> getHcLotInstanceDetail(@RequestParam("id") Long id) {
        HcLotInstanceDO instance = hcLotInstanceService.getLotInstance(id);
        if (instance == null) {
            throw invalidParamException("批次实例不存在");
        }
        return success(toRespVO(instance));
    }

    private HcLotInstanceRespVO toRespVO(HcLotInstanceDO instance) {
        HcLotInstanceRespVO respVO = BeanUtils.toBean(instance, HcLotInstanceRespVO.class);
        HcLotRuleDO rule = instance.getRuleId() == null ? null : hcLotRuleService.getHcLotRule(instance.getRuleId());
        if (rule != null) {
            respVO.setRuleName(rule.getRuleName());
            if (respVO.getRuleVersion() == null) {
                respVO.setRuleVersion(rule.getVersionNo());
            }
            if (StrUtil.isBlank(respVO.getBizType())) {
                respVO.setBizType(rule.getBizType());
            }
            if (StrUtil.isBlank(respVO.getProductCategoryCode())) {
                respVO.setProductCategoryCode(rule.getProductCategoryCode());
            }
            if (StrUtil.isBlank(respVO.getProdType())) {
                respVO.setProdType(rule.getProdType());
            }
        }
        Map<String, Object> formatSnapshot = parseObjectMap(instance.getRuleFormatSnapshotJson());
        respVO.setRuleFormatSnapshot(formatSnapshot);
        respVO.setRuleFormatSummary(stringValue(formatSnapshot.get("formatSummary")));
        respVO.setSegmentValues(parseObjectMap(instance.getSegmentValuesJson()));
        respVO.setGenerationContext(parseObjectMap(instance.getContextJson()));
        respVO.setProductionAttributes(parseObjectMap(instance.getAttributeJson()));
        return respVO;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObjectMap(String value) {
        if (StrUtil.isBlank(value)) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = JsonUtils.parseObject(value, Map.class);
            return parsed == null ? Map.of() : new LinkedHashMap<>(parsed);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
