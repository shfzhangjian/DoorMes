package cn.iocoder.yudao.module.mes.service.hc.processanalysis;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisFieldRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processanalysis.HcProcessAnalysisConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processanalysis.HcProcessAnalysisConfigMapper;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProcessAnalysisServiceImpl implements HcProcessAnalysisService {

    private static final String SCOPE_PRIVATE = "PRIVATE";
    private static final String SCOPE_SHARED = "SHARED";
    private static final int CONFIG_VERSION = 1;
    private static final int CONFIG_JSON_MAX_BYTES = 200 * 1024;

    private static final List<HcProcessAnalysisFieldRespVO> FIELD_CATALOG = buildFieldCatalog();

    @Resource
    private HcProcessAnalysisConfigMapper hcProcessAnalysisConfigMapper;

    @Override
    public List<HcProcessAnalysisFieldRespVO> getFieldCatalog() {
        return FIELD_CATALOG;
    }

    @Override
    public List<HcProcessAnalysisConfigRespVO> getVisibleConfigList() {
        Long loginUserId = requireLoginUserId();
        return hcProcessAnalysisConfigMapper.selectVisibleList(loginUserId).stream()
                .map(item -> buildConfigResp(item, loginUserId))
                .toList();
    }

    @Override
    public HcProcessAnalysisConfigRespVO getVisibleConfig(Long id) {
        Long loginUserId = requireLoginUserId();
        return buildConfigResp(validateVisibleConfig(id, loginUserId), loginUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(HcProcessAnalysisConfigSaveReqVO reqVO) {
        Long loginUserId = requireLoginUserId();
        normalizeAndValidate(reqVO);
        validateNameUnique(loginUserId, reqVO.getConfigName(), null);
        if (Boolean.TRUE.equals(reqVO.getDefaultFlag())) {
            hcProcessAnalysisConfigMapper.clearDefaultFlags(loginUserId, null);
        }
        HcProcessAnalysisConfigDO entity = BeanUtils.toBean(reqVO, HcProcessAnalysisConfigDO.class);
        entity.setOwnerUserId(loginUserId);
        entity.setConfigVersion(CONFIG_VERSION);
        entity.setTenantId(TenantContextHolder.getTenantId());
        hcProcessAnalysisConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(HcProcessAnalysisConfigSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw invalidParamException("更新分析方案时方案ID不能为空");
        }
        Long loginUserId = requireLoginUserId();
        validateOwnedConfig(reqVO.getId(), loginUserId);
        normalizeAndValidate(reqVO);
        validateNameUnique(loginUserId, reqVO.getConfigName(), reqVO.getId());
        if (Boolean.TRUE.equals(reqVO.getDefaultFlag())) {
            hcProcessAnalysisConfigMapper.clearDefaultFlags(loginUserId, reqVO.getId());
        }
        HcProcessAnalysisConfigDO updateObj = BeanUtils.toBean(reqVO, HcProcessAnalysisConfigDO.class);
        updateObj.setConfigVersion(CONFIG_VERSION);
        hcProcessAnalysisConfigMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        Long loginUserId = requireLoginUserId();
        validateOwnedConfig(id, loginUserId);
        hcProcessAnalysisConfigMapper.deleteById(id);
    }

    private void normalizeAndValidate(HcProcessAnalysisConfigSaveReqVO reqVO) {
        reqVO.setConfigName(StrUtil.trim(reqVO.getConfigName()));
        reqVO.setScopeType(normalizeScopeType(reqVO.getScopeType()));
        reqVO.setDefaultFlag(Boolean.TRUE.equals(reqVO.getDefaultFlag()));
        reqVO.setConfigVersion(CONFIG_VERSION);
        reqVO.setRemark(StrUtil.trim(reqVO.getRemark()));
        if (reqVO.getConfigJson().getBytes(StandardCharsets.UTF_8).length > CONFIG_JSON_MAX_BYTES) {
            throw invalidParamException("分析方案配置不能超过200KB");
        }
        if (!JsonUtils.isJsonObject(reqVO.getConfigJson())) {
            throw invalidParamException("分析方案配置必须是合法JSON对象");
        }
    }

    private String normalizeScopeType(String scopeType) {
        String normalized = StrUtil.blankToDefault(scopeType, SCOPE_PRIVATE).trim().toUpperCase(Locale.ROOT);
        if (!SCOPE_PRIVATE.equals(normalized) && !SCOPE_SHARED.equals(normalized)) {
            throw invalidParamException("分析方案范围仅支持个人或共享");
        }
        return normalized;
    }

    private void validateNameUnique(Long ownerUserId, String configName, Long excludeId) {
        if (hcProcessAnalysisConfigMapper.selectByOwnerAndName(ownerUserId, configName, excludeId) != null) {
            throw invalidParamException("分析方案名称已存在：{}", configName);
        }
    }

    private HcProcessAnalysisConfigDO validateVisibleConfig(Long id, Long loginUserId) {
        HcProcessAnalysisConfigDO entity = validateConfigExists(id);
        if (!Objects.equals(entity.getOwnerUserId(), loginUserId)
                && !SCOPE_SHARED.equals(entity.getScopeType())) {
            throw invalidParamException("无权查看该分析方案");
        }
        return entity;
    }

    private HcProcessAnalysisConfigDO validateOwnedConfig(Long id, Long loginUserId) {
        HcProcessAnalysisConfigDO entity = validateConfigExists(id);
        if (!Objects.equals(entity.getOwnerUserId(), loginUserId)) {
            throw invalidParamException("只有方案所有人可以修改或删除");
        }
        return entity;
    }

    private HcProcessAnalysisConfigDO validateConfigExists(Long id) {
        HcProcessAnalysisConfigDO entity = hcProcessAnalysisConfigMapper.selectById(id);
        if (entity == null) {
            throw invalidParamException("分析方案不存在");
        }
        return entity;
    }

    private HcProcessAnalysisConfigRespVO buildConfigResp(
            HcProcessAnalysisConfigDO entity, Long loginUserId) {
        HcProcessAnalysisConfigRespVO respVO =
                BeanUtils.toBean(entity, HcProcessAnalysisConfigRespVO.class);
        respVO.setEditable(Objects.equals(entity.getOwnerUserId(), loginUserId));
        return respVO;
    }

    private Long requireLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw invalidParamException("请先登录后再维护分析方案");
        }
        return loginUserId;
    }

    private static List<HcProcessAnalysisFieldRespVO> buildFieldCatalog() {
        List<HcProcessAnalysisFieldRespVO> fields = new ArrayList<>();
        fields.add(dimension("planNo", "计划号", "计划", "TEXT", "planNo", true, "生产计划编号"));
        fields.add(dimension("planStatus", "计划状态", "计划", "TEXT", "planStatus", true, "生产计划当前状态"));
        fields.add(dimension("postProcessFlag", "是否后加工", "计划", "BOOLEAN", "postProcessFlag", true,
                "计划是否挂接半成品库存继续后加工"));
        fields.add(dimension("planNoTagText", "计划标签", "计划", "TEXT", "planNoTagText", false, "生产进度计划号标签"));
        fields.add(time("planDate", "计划日期", "计划", "DATE", "planDate", true, "计划日期"));
        fields.add(time("productionStartDate", "生产开始日期", "计划", "DATE", "productionStartDate", true,
                "计划生产开始日期"));
        fields.add(time("productionEndDate", "生产结束日期", "计划", "DATE", "productionEndDate", true,
                "计划生产结束日期"));
        fields.add(dimension("materialCode", "生产料号", "物料批次", "TEXT", "materialCode", true, "生产物料编码"));
        fields.add(dimension("materialName", "生产物料名称", "物料批次", "TEXT", "materialName", true, "生产物料名称"));
        fields.add(dimension("motherMaterialCode", "母料料号", "物料批次", "TEXT", "motherMaterialCode", true,
                "母料物料编码"));
        fields.add(dimension("motherMaterialName", "母料名称", "物料批次", "TEXT", "motherMaterialName", true,
                "母料名称"));
        fields.add(dimension("motherModelCode", "母料型号", "物料批次", "TEXT", "motherModelCode", true, "母料型号编码"));
        fields.add(dimension("motherModelName", "母料型号名称", "物料批次", "TEXT", "motherModelName", true,
                "母料型号名称"));
        fields.add(dimension("modelCode", "成品型号", "物料批次", "TEXT", "modelCode", true, "计划成品型号"));
        fields.add(dimension("modelName", "成品型号名称", "物料批次", "TEXT", "modelName", true, "计划成品型号名称"));
        fields.add(dimension("actualModelCode", "实际型号", "物料批次", "TEXT", "actualModelCode", true,
                "按生产进度差异分组后的实际显示型号"));
        fields.add(dimension("sizeSpec", "尺寸规格", "物料批次", "TEXT", "sizeSpec", true, "计划尺寸规格"));
        fields.add(dimension("sizeName", "尺寸名称", "物料批次", "TEXT", "sizeName", true, "计划尺寸名称"));
        fields.add(dimension("actualSizeSpec", "实际尺寸", "物料批次", "TEXT", "actualSizeSpec", true,
                "按生产进度差异分组后的实际显示尺寸"));
        fields.add(dimension("targetUom", "计划单位", "物料批次", "TEXT", "targetUom", false, "计划目标量单位"));
        fields.add(dimension("batchNo", "主批号", "物料批次", "TEXT", "batchNo", true, "计划主批号"));
        fields.add(dimension("productionBatchNo", "生产批号", "物料批次", "TEXT", "productionBatchNo", true,
                "计划生产批号"));
        fields.add(dimension("parentProductionBatchNo", "父生产批号", "物料批次", "TEXT",
                "parentProductionBatchNo", true, "计划父生产批号"));
        fields.add(dimension("motherRollBatchNo", "母批号", "物料批次", "TEXT", "motherRollBatchNo", true,
                "生产进度统一母卷批号"));
        fields.add(dimension("segmentBatchNo", "分段批号", "物料批次", "TEXT", "segmentBatchNo", true,
                "生产进度追踪分段批号"));
        fields.add(dimension("variationStartStageCode", "差异起始工序编码", "工序", "TEXT",
                "variationStartStageCode", true, "型号尺寸差异开始的工序编码"));
        fields.add(dimension("variationStartStageName", "差异起始工序", "工序", "TEXT",
                "variationStartStageName", true, "型号尺寸差异开始的工序名称"));
        fields.add(dimension("stageCode", "工序编码", "工序", "TEXT", "stages.*.stageCode", true, "工序阶段编码"));
        fields.add(dimension("stageName", "工序名称", "工序", "TEXT", "stages.*.stageName", true, "工序阶段名称"));
        fields.add(dimension("stageStatus", "工序状态", "工序", "TEXT", "stages.*.stageStatus", true,
                "未开始、待加工、进行中或已完成"));
        fields.add(dimension("sourceBatchNos", "工序来源批号", "工序", "TEXT", "stages.*.sourceBatchNos", false,
                "工序聚合来源批号"));
        fields.add(dimension("outputBatchNos", "工序产出批号", "工序", "TEXT", "stages.*.outputBatchNos", false,
                "工序聚合产出批号"));
        fields.add(dimension("reportUnit", "报工单位", "工序", "TEXT", "stages.*.reportUnit", false, "工序计量单位"));
        fields.add(dimension("pendingUnit", "未加工单位", "工序", "TEXT", "stages.*.pendingUnit", false,
                "工序未加工数量单位"));
        fields.add(dimension("lengthUnit", "长度单位", "工序", "TEXT", "stages.*.lengthUnit", false, "折算长度单位"));
        fields.add(dimension("stageRemark", "工序备注", "工序", "TEXT", "stages.*.remark", false, "工序阶段备注"));

        fields.add(metric("targetQty", "计划目标量", "指标", "targetQty", "planMergeKey", "计划级目标量"));
        fields.add(metric("netPlanQty", "净排产量", "指标", "netPlanQty", "planMergeKey", "计划级净排产量"));
        fields.add(metric("totalDefectQty", "显示行累计损耗", "指标", "totalDefectQty", "pivotRowKey",
                "生产进度显示行各工序损耗合计"));
        fields.add(metric("inputQty", "投入量", "指标", "stages.*.inputQty", "stageGrainKey", "工序投入量"));
        fields.add(metric("reportQty", "报工量", "指标", "stages.*.reportQty", "stageGrainKey", "工序报工量"));
        fields.add(metric("doneQty", "完工量", "指标", "stages.*.doneQty", "stageGrainKey", "工序完工量"));
        fields.add(metric("pendingQty", "未加工量", "指标", "stages.*.pendingQty", "stageGrainKey", "工序未加工量"));
        fields.add(metric("defectQty", "不合格/损耗量", "指标", "stages.*.defectQty", "stageGrainKey",
                "工序不合格或损耗量"));
        fields.add(metric("inspectionQty", "送检数量", "指标", "stages.*.inspectionQty", "stageGrainKey",
                "工序送检数量"));
        fields.add(metric("inspectionNgQty", "检验NG数量", "指标", "stages.*.inspectionNgQty", "stageGrainKey",
                "工序检验NG数量"));
        fields.add(metric("confirmedQty", "已确认数量", "指标", "stages.*.confirmedQty", "stageGrainKey",
                "工序已确认数量"));
        fields.add(metric("lengthQty", "折算长度", "指标", "stages.*.lengthQty", "stageGrainKey", "工序折算长度"));
        fields.add(metric("processLength", "二次磨皮长度", "指标", "stages.*.processLength", "stageGrainKey",
                "二次磨皮加工长度"));
        fields.add(metricWithAggregation("startPosition", "二次磨皮起位置", "指标", "stages.*.startPosition",
                "MAX", "stageGrainKey", "二次磨皮加工起位置"));
        fields.add(metric("pieceCount", "片级记录数", "指标", "stages.*.pieceDetails.length", "stageGrainKey",
                "工序片级明细数量"));
        fields.add(metric("pieceNgCount", "不良片数", "指标", "stages.*.pieceDetails[status=NG].length",
                "stageGrainKey", "工序片级明细中NG数量"));
        fields.add(metric("inspectionRecordCount", "检验记录数", "指标", "stages.*.inspectionDetails.length",
                "stageGrainKey", "工序检验明细数量"));
        fields.add(metric("blackDotCount", "黑点", "指标", "quality.summaryRows[].blackDotCount",
                "qualityGrainKey", "分段工序中存在黑点现象的片数"));
        fields.add(metric("blueDotCount", "蓝点", "指标", "quality.summaryRows[].blueDotCount",
                "qualityGrainKey", "分段工序中存在蓝点现象的片数"));
        fields.add(metric("yellowDotCount", "黄点", "指标", "quality.summaryRows[].yellowDotCount",
                "qualityGrainKey", "分段工序中存在黄点现象的片数"));
        fields.add(metric("redDotCount", "红点", "指标", "quality.summaryRows[].redDotCount",
                "qualityGrainKey", "分段工序中存在红点现象的片数"));
        fields.add(metric("pinholeCount", "针孔", "指标", "quality.summaryRows[].pinholeCount",
                "qualityGrainKey", "分段工序中存在针孔现象的片数"));
        fields.add(metric("stripeCount", "条纹", "指标", "quality.summaryRows[].stripeCount",
                "qualityGrainKey", "分段工序中存在条纹现象的片数"));
        fields.add(metric("wrinkleCount", "褶皱", "指标", "quality.summaryRows[].wrinkleCount",
                "qualityGrainKey", "分段工序中存在褶皱现象的片数"));
        fields.add(metric("waveCount", "波浪纹", "指标", "quality.summaryRows[].waveCount",
                "qualityGrainKey", "分段工序中存在波浪纹现象的片数"));
        fields.add(metric("otherCount", "其他", "指标", "quality.summaryRows[].otherCount",
                "qualityGrainKey", "分段工序中其他片级缺陷现象数量"));

        fields.add(time("latestReportTime", "计划最后报工时间", "时间", "DATETIME", "latestReportTime", true,
                "生产进度显示行各工序最大有效时间"));
        fields.add(time("lastReportTime", "工序最后报工时间", "时间", "DATETIME",
                "stages.*.lastReportTime", true, "工序最大有效报工时间"));

        fields.add(detail("pieceNo", "生产片号", "TEXT", "stages.*.pieceDetails[].pieceNo",
                "用于分段末级片号列表，不作为透视行列维度"));
        fields.add(detail("pieceActualModelCode", "片实际型号", "TEXT",
                "stages.*.pieceDetails[].actualModelCode", "片级实际型号"));
        fields.add(detail("pieceActualSizeSpec", "片实际尺寸", "TEXT",
                "stages.*.pieceDetails[].actualSizeSpec", "片级实际尺寸"));
        fields.add(detail("pieceSourceBatchNo", "片来源批号", "TEXT",
                "stages.*.pieceDetails[].sourceBatchNo", "片级来源批号"));
        fields.add(detail("pieceOutputBatchNo", "片产出批号", "TEXT",
                "stages.*.pieceDetails[].outputBatchNo", "片级产出批号"));
        fields.add(detail("pieceStatus", "片状态", "TEXT", "stages.*.pieceDetails[].status", "片级状态"));
        fields.add(detail("coaFlag", "COA标记", "BOOLEAN", "stages.*.pieceDetails[].coaFlag", "片级COA标记"));
        fields.add(detail("defectFlag", "片缺陷标记", "BOOLEAN",
                "stages.*.pieceDetails[].defectFlag", "片级缺陷标记"));
        fields.add(detail("reportConfirmed", "片报工已确认", "BOOLEAN",
                "stages.*.pieceDetails[].reportConfirmed", "片级报工确认标记"));
        fields.add(detail("pieceRemark", "片备注", "TEXT",
                "stages.*.pieceDetails[].remark", "片级备注或缺陷摘要"));
        fields.add(detail("pieceStageCode", "片工序编码", "TEXT",
                "stages.*.pieceDetails[].stageCode", "片级来源工序编码"));
        fields.add(detail("pieceLastReportTime", "片最后报工时间", "DATETIME",
                "stages.*.pieceDetails[].lastReportTime", "片级最后报工时间"));
        fields.add(detail("inspectionId", "检验ID", "NUMBER",
                "stages.*.inspectionDetails[].inspectionId", "检验记录主键"));
        fields.add(detail("inspectionNo", "检验单号", "TEXT",
                "stages.*.inspectionDetails[].inspectionNo", "检验钻取单号"));
        fields.add(detail("inspectionType", "检验类型", "TEXT",
                "stages.*.inspectionDetails[].inspectionType", "检验类型"));
        fields.add(detail("inspectionSourceType", "检验来源类型", "TEXT",
                "stages.*.inspectionDetails[].sourceType", "检验来源类型"));
        fields.add(detail("inspectionStageCode", "检验工序编码", "TEXT",
                "stages.*.inspectionDetails[].stageCode", "检验来源工序编码"));
        fields.add(detail("inspectionProductBatchNo", "检验产品批号", "TEXT",
                "stages.*.inspectionDetails[].productBatchNo", "检验产品批号"));
        fields.add(detail("inspectionStatus", "检验状态", "TEXT",
                "stages.*.inspectionDetails[].status", "检验单状态"));
        fields.add(detail("judgment", "检验判定", "TEXT",
                "stages.*.inspectionDetails[].judgment", "检验判定"));
        fields.add(detail("inspectionDefectSummary", "检验缺陷摘要", "TEXT",
                "stages.*.inspectionDetails[].defectSummary", "检验缺陷摘要"));
        fields.add(detail("inspectionRemark", "检验备注", "TEXT",
                "stages.*.inspectionDetails[].remark", "检验备注"));
        fields.add(detail("detailInspectionQty", "明细送检数量", "NUMBER",
                "stages.*.inspectionDetails[].inspectionQty", "检验明细送检数量"));
        fields.add(detail("detailInspectionNgQty", "明细检验NG数量", "NUMBER",
                "stages.*.inspectionDetails[].inspectionNgQty", "检验明细NG数量"));
        fields.add(detail("inspectionTime", "检验时间", "DATETIME",
                "stages.*.inspectionDetails[].inspectionTime", "检验明细时间"));
        return List.copyOf(fields);
    }

    private static HcProcessAnalysisFieldRespVO dimension(
            String code, String label, String category, String dataType,
            String sourcePath, boolean drillable, String description) {
        return field(code, label, category, dataType, "DIMENSION", sourcePath,
                null, null, drillable, description);
    }

    private static HcProcessAnalysisFieldRespVO time(
            String code, String label, String category, String dataType,
            String sourcePath, boolean drillable, String description) {
        return field(code, label, category, dataType, "TIME", sourcePath,
                null, null, drillable, description);
    }

    private static HcProcessAnalysisFieldRespVO metric(
            String code, String label, String category, String sourcePath,
            String distinctKeyField, String description) {
        return metricWithAggregation(code, label, category, sourcePath,
                "SUM_DISTINCT", distinctKeyField, description);
    }

    private static HcProcessAnalysisFieldRespVO metricWithAggregation(
            String code, String label, String category, String sourcePath,
            String aggregation, String distinctKeyField, String description) {
        return field(code, label, category, "NUMBER", "METRIC", sourcePath,
                aggregation, distinctKeyField, false, description);
    }

    private static HcProcessAnalysisFieldRespVO detail(
            String code, String label, String dataType, String sourcePath, String description) {
        return field(code, label, "来源明细", dataType, "DETAIL", sourcePath,
                null, null, false, description);
    }

    private static HcProcessAnalysisFieldRespVO field(
            String code, String label, String category, String dataType, String role,
            String sourcePath, String defaultAggregation, String distinctKeyField,
            boolean drillable, String description) {
        return HcProcessAnalysisFieldRespVO.builder()
                .code(code)
                .label(label)
                .category(category)
                .dataType(dataType)
                .role(role)
                .sourcePath(sourcePath)
                .defaultAggregation(defaultAggregation)
                .distinctKeyField(distinctKeyField)
                .filterOperators(resolveFilterOperators(dataType, role))
                .drillable(drillable)
                .description(description)
                .build();
    }

    private static List<String> resolveFilterOperators(String dataType, String role) {
        if ("DETAIL".equals(role)) {
            return List.of();
        }
        return switch (dataType) {
            case "NUMBER" -> List.of("EQ", "NE", "GT", "GTE", "LT", "LTE", "BETWEEN", "EMPTY", "NOT_EMPTY");
            case "DATE", "DATETIME" -> List.of("EQ", "GTE", "LTE", "BETWEEN", "EMPTY", "NOT_EMPTY");
            case "BOOLEAN" -> List.of("EQ", "NE", "EMPTY", "NOT_EMPTY");
            default -> List.of("EQ", "NE", "CONTAINS", "NOT_CONTAINS", "IN", "EMPTY", "NOT_EMPTY");
        };
    }

}
