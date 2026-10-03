package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 生产记录垫型解析器。
 *
 * <p>垫型的业务主数据来源是产品型号的 {@code categoryCode}。生产记录仅接受黑垫、白垫两个
 * 可追溯业务值，未维护或非垫型分类的型号返回 {@code null}，由页面归入“未归类”。</p>
 */
@Component
public class HcProductionRecordPadTypeResolver {

    public static final String BLACK_PAD = "BLACK_PAD";
    public static final String WHITE_PAD = "WHITE_PAD";
    public static final String UNCLASSIFIED = "UNCLASSIFIED";

    @Resource
    private HcProductModelMapper productModelMapper;

    public String resolveByModelCode(String modelCode) {
        if (StrUtil.isBlank(modelCode)) {
            return null;
        }
        HcProductModelDO model = productModelMapper.selectByModelCode(modelCode.trim());
        return model == null ? null : normalizePadType(model.getCategoryCode());
    }

    /**
     * 湿法导布研发登记以导布记录所属产线为垫型事实，研发型号仅用于追溯。
     */
    public String resolveByGuideClothLineCode(String lineCode) {
        if (StrUtil.isBlank(lineCode)) {
            return null;
        }
        return switch (lineCode.trim().toUpperCase()) {
            case "BLACK", "B" -> BLACK_PAD;
            case "WHITE", "W" -> WHITE_PAD;
            default -> null;
        };
    }

    public Map<String, String> resolveByModelCodes(Collection<String> modelCodes) {
        List<String> normalizedCodes = modelCodes == null ? List.of() : modelCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        productModelMapper.selectList(new LambdaQueryWrapperX<HcProductModelDO>()
                        .in(HcProductModelDO::getModelCode, normalizedCodes)
                        .eq(HcProductModelDO::getDeleted, false))
                .forEach(model -> result.put(model.getModelCode(), normalizePadType(model.getCategoryCode())));
        return result;
    }

    public boolean matchesFilter(String filterPadType, String actualPadType) {
        String normalizedFilter = normalizeFilter(filterPadType);
        if (normalizedFilter == null) {
            return true;
        }
        if (UNCLASSIFIED.equals(normalizedFilter)) {
            return actualPadType == null;
        }
        return normalizedFilter.equals(actualPadType);
    }

    public String normalizePadType(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return BLACK_PAD.equals(normalized) || WHITE_PAD.equals(normalized) ? normalized : null;
    }

    public String normalizeFilter(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (UNCLASSIFIED.equals(normalized)) {
            return normalized;
        }
        return normalizePadType(normalized);
    }
}
