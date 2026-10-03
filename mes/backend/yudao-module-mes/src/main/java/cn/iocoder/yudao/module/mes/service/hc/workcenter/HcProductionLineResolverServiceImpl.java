package cn.iocoder.yudao.module.mes.service.hc.workcenter;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.workcenter.HcWorkCenterMapper;
import jakarta.annotation.Resource;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcProductionLineResolverServiceImpl implements HcProductionLineResolverService {

    private static final Integer STATUS_ENABLED = 0;
    private static final String LINE_WHITE = "WHITE";
    private static final String LINE_BLACK = "BLACK";

    @Resource
    private HcWorkCenterMapper hcWorkCenterMapper;

    @Override
    public HcProductionLineContext resolveByWorkCenterId(Long workCenterId) {
        if (workCenterId == null) {
            return defaultLineContext(null);
        }
        HcWorkCenterDO workCenter = hcWorkCenterMapper.selectById(workCenterId);
        return workCenter == null ? defaultLineContext(null) : buildContext(workCenter);
    }

    @Override
    public HcProductionLineContext resolveByWorkCenterCode(String workCenterCode) {
        if (StrUtil.isBlank(workCenterCode)) {
            return defaultLineContext(null);
        }
        HcWorkCenterDO workCenter = hcWorkCenterMapper.selectOne(new LambdaQueryWrapperX<HcWorkCenterDO>()
                .eq(HcWorkCenterDO::getWcCode, workCenterCode)
                .last("LIMIT 1"));
        return workCenter == null ? defaultLineContext(null) : buildContext(workCenter);
    }

    @Override
    public HcProductionLineContext resolveByMotherModelCode(String motherModelCode) {
        String lineCode = inferLineCodeByModel(motherModelCode);
        HcWorkCenterDO workCenter = findPreferredWorkCenterByLineCode(lineCode);
        return workCenter == null ? defaultLineContext(lineCode) : buildContext(workCenter);
    }

    private HcWorkCenterDO findPreferredWorkCenterByLineCode(String lineCode) {
        if (StrUtil.isBlank(lineCode)) {
            return null;
        }
        return hcWorkCenterMapper.selectOne(new LambdaQueryWrapperX<HcWorkCenterDO>()
                .eq(HcWorkCenterDO::getStatus, STATUS_ENABLED)
                .eq(HcWorkCenterDO::getLineCode, lineCode)
                .orderByAsc(HcWorkCenterDO::getLineSort)
                .orderByAsc(HcWorkCenterDO::getWcCode)
                .last("LIMIT 1"));
    }

    private HcProductionLineContext buildContext(HcWorkCenterDO workCenter) {
        String lineCode = normalizeLineCode(workCenter.getLineCode());
        String lineShortCode = firstNotBlank(workCenter.getLineShortCode(), defaultLineShortCode(lineCode));
        String batchLineCode = firstNotBlank(workCenter.getBatchLineCode(), defaultBatchLineCode(lineCode));
        return HcProductionLineContext.builder()
                .workCenterId(workCenter.getId())
                .workCenterCode(workCenter.getWcCode())
                .workCenterName(workCenter.getWcName())
                .lineCode(lineCode)
                .lineName(firstNotBlank(workCenter.getLineName(), defaultLineName(lineCode)))
                .lineShortCode(lineShortCode)
                .batchLineCode(batchLineCode)
                .build();
    }

    private HcProductionLineContext defaultLineContext(String lineCode) {
        String normalizedLineCode = normalizeLineCode(lineCode);
        return HcProductionLineContext.builder()
                .lineCode(normalizedLineCode)
                .lineName(defaultLineName(normalizedLineCode))
                .lineShortCode(defaultLineShortCode(normalizedLineCode))
                .batchLineCode(defaultBatchLineCode(normalizedLineCode))
                .build();
    }

    private String inferLineCodeByModel(String modelCode) {
        String code = modelCode == null ? "" : modelCode.trim().toUpperCase(Locale.ROOT);
        if (code.startsWith("W")) {
            return LINE_WHITE;
        }
        if (code.startsWith("B") || code.startsWith("C")) {
            return LINE_BLACK;
        }
        return null;
    }

    private String normalizeLineCode(String lineCode) {
        String code = lineCode == null ? "" : lineCode.trim().toUpperCase(Locale.ROOT);
        if ("W".equals(code)) {
            return LINE_WHITE;
        }
        if ("B".equals(code) || "C".equals(code)) {
            return LINE_BLACK;
        }
        return StrUtil.isBlank(code) ? "UNKNOWN" : code;
    }

    private String defaultLineName(String lineCode) {
        return switch (normalizeLineCode(lineCode)) {
            case LINE_WHITE -> "白垫线";
            case LINE_BLACK -> "黑垫线";
            default -> "未知产线";
        };
    }

    private String defaultLineShortCode(String lineCode) {
        return switch (normalizeLineCode(lineCode)) {
            case LINE_WHITE -> "W";
            case LINE_BLACK -> "B";
            default -> "UNKNOWN";
        };
    }

    private String defaultBatchLineCode(String lineCode) {
        return switch (normalizeLineCode(lineCode)) {
            case LINE_WHITE -> "A";
            case LINE_BLACK -> "B";
            default -> "UNKNOWN";
        };
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
