package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldTargetConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldTargetConfigMapper;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsYieldTargetConfigServiceImpl implements QmsYieldTargetConfigService {

    public static final String TARGET_TYPE_OUTPUT_QTY = "OUTPUT_QTY";
    public static final String TARGET_TYPE_YIELD_RATE = "YIELD_RATE";
    private static final Set<String> BASE_PROCESS_CODES = Set.of("FORMULA", "WET");

    private static final ErrorCode QMS_YIELD_TARGET_CONFIG_NOT_EXISTS =
            new ErrorCode(1008100180, "工序理论产量配置不存在");
    private static final ErrorCode QMS_YIELD_TARGET_CONFIG_DUPLICATE =
            new ErrorCode(1008100181, "同一产品型号和基础工序的目标配置已存在");
    private static final ErrorCode QMS_YIELD_TARGET_CONFIG_SEGMENT_COUNT_INVALID =
            new ErrorCode(1008100182, "理论产量只需维护配料和湿法，后续工序由母卷统计公式自动计算");

    private static final Map<String, ProcessOption> PROCESS_OPTIONS = new LinkedHashMap<>();

    static {
        PROCESS_OPTIONS.put("FORMULA", new ProcessOption("配料", "kg", TARGET_TYPE_OUTPUT_QTY));
        PROCESS_OPTIONS.put("WET", new ProcessOption("湿法", "m", TARGET_TYPE_OUTPUT_QTY));
    }

    @Resource
    private QmsYieldTargetConfigMapper qmsYieldTargetConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTargetConfig(QmsYieldTargetConfigSaveReqVO createReqVO) {
        QmsYieldTargetConfigDO entity = toDO(createReqVO);
        validateDuplicate(entity.getModelCode(), entity.getProcessCode(), entity.getSegmentCount(), null);
        qmsYieldTargetConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTargetConfig(QmsYieldTargetConfigSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        QmsYieldTargetConfigDO updateObj = toDO(updateReqVO);
        updateObj.setId(updateReqVO.getId());
        validateDuplicate(updateObj.getModelCode(), updateObj.getProcessCode(), updateObj.getSegmentCount(),
                updateObj.getId());
        qmsYieldTargetConfigMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTargetConfig(Long id) {
        validateExists(id);
        qmsYieldTargetConfigMapper.deleteById(id);
    }

    @Override
    public QmsYieldTargetConfigRespVO getTargetConfig(Long id) {
        return toResp(validateExists(id));
    }

    @Override
    public PageResult<QmsYieldTargetConfigRespVO> getTargetConfigPage(QmsYieldTargetConfigPageReqVO pageReqVO) {
        normalizePageReq(pageReqVO);
        PageResult<QmsYieldTargetConfigDO> pageResult = qmsYieldTargetConfigMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toResp).collect(Collectors.toList()),
                pageResult.getTotal());
    }

    @Override
    public List<QmsYieldTargetConfigRespVO> getSimpleList() {
        return qmsYieldTargetConfigMapper.selectEnabledList().stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    public List<QmsYieldTargetConfigExcelVO> getExportList(QmsYieldTargetConfigPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return qmsYieldTargetConfigMapper.selectList(reqVO).stream().map(this::toExcel).collect(Collectors.toList());
    }

    private QmsYieldTargetConfigDO validateExists(Long id) {
        QmsYieldTargetConfigDO entity = qmsYieldTargetConfigMapper.selectById(id);
        if (entity == null) {
            throw exception(QMS_YIELD_TARGET_CONFIG_NOT_EXISTS);
        }
        return entity;
    }

    private void validateDuplicate(String modelCode, String processCode, Integer segmentCount, Long excludeId) {
        if (qmsYieldTargetConfigMapper.selectByModelAndProcess(modelCode, processCode, segmentCount, excludeId) != null) {
            throw exception(QMS_YIELD_TARGET_CONFIG_DUPLICATE);
        }
    }

    private void normalizePageReq(QmsYieldTargetConfigPageReqVO reqVO) {
        reqVO.setModelCode(normalizeModelCode(reqVO.getModelCode()));
        reqVO.setProcessCode(normalizeProcessCode(reqVO.getProcessCode()));
    }

    private QmsYieldTargetConfigDO toDO(QmsYieldTargetConfigSaveReqVO reqVO) {
        String processCode = normalizeProcessCode(reqVO.getProcessCode());
        ProcessOption option = PROCESS_OPTIONS.get(processCode);
        if (!BASE_PROCESS_CODES.contains(processCode)) {
            throw exception(QMS_YIELD_TARGET_CONFIG_SEGMENT_COUNT_INVALID);
        }
        QmsYieldTargetConfigDO entity = new QmsYieldTargetConfigDO();
        entity.setModelCode(normalizeModelCode(reqVO.getModelCode()));
        entity.setProcessCode(processCode);
        entity.setProcessName(StrUtil.blankToDefault(StrUtil.trim(reqVO.getProcessName()),
                option == null ? processCode : option.name()));
        entity.setMeasureUnit(resolveMeasureUnit(StrUtil.trim(reqVO.getMeasureUnit()), option));
        entity.setTargetType(normalizeTargetType(reqVO.getTargetType(), option));
        entity.setSegmentCount(normalizeSegmentCount(entity.getModelCode(), reqVO.getSegmentCount()));
        entity.setTargetQualifiedQty(reqVO.getTargetQualifiedQty());
        entity.setStatus(reqVO.getStatus() == null ? 0 : reqVO.getStatus());
        entity.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        entity.setRemark(StrUtil.trim(reqVO.getRemark()));
        return entity;
    }

    private QmsYieldTargetConfigRespVO toResp(QmsYieldTargetConfigDO entity) {
        QmsYieldTargetConfigRespVO respVO = new QmsYieldTargetConfigRespVO();
        respVO.setId(entity.getId());
        respVO.setModelCode(entity.getModelCode());
        respVO.setProcessCode(entity.getProcessCode());
        respVO.setProcessName(entity.getProcessName());
        respVO.setMeasureUnit(resolveMeasureUnit(entity.getMeasureUnit(), PROCESS_OPTIONS.get(entity.getProcessCode())));
        respVO.setTargetType(normalizeTargetType(entity.getTargetType(), PROCESS_OPTIONS.get(entity.getProcessCode())));
        respVO.setSegmentCount(defaultSegmentCount(entity.getSegmentCount()));
        respVO.setTargetQualifiedQty(entity.getTargetQualifiedQty());
        respVO.setStatus(entity.getStatus());
        respVO.setSort(entity.getSort());
        respVO.setRemark(entity.getRemark());
        respVO.setCreateTime(entity.getCreateTime());
        return respVO;
    }

    private QmsYieldTargetConfigExcelVO toExcel(QmsYieldTargetConfigDO entity) {
        QmsYieldTargetConfigExcelVO excelVO = new QmsYieldTargetConfigExcelVO();
        excelVO.setModelCode(entity.getModelCode());
        excelVO.setProcessCode(entity.getProcessCode());
        excelVO.setProcessName(entity.getProcessName());
        excelVO.setMeasureUnit(resolveMeasureUnit(entity.getMeasureUnit(), PROCESS_OPTIONS.get(entity.getProcessCode())));
        excelVO.setTargetTypeName(targetTypeName(normalizeTargetType(entity.getTargetType(),
                PROCESS_OPTIONS.get(entity.getProcessCode()))));
        excelVO.setSegmentCountName(segmentCountName(entity.getModelCode(), entity.getSegmentCount()));
        excelVO.setTargetQualifiedQty(entity.getTargetQualifiedQty());
        excelVO.setStatusName(entity.getStatus() != null && entity.getStatus() == 0 ? "启用" : "禁用");
        excelVO.setSort(entity.getSort());
        excelVO.setRemark(entity.getRemark());
        return excelVO;
    }

    private String normalizeModelCode(String modelCode) {
        if (StrUtil.isBlank(modelCode)) {
            return "";
        }
        String text = StrUtil.trim(modelCode).toUpperCase(Locale.ROOT);
        return text.length() <= 3 ? text : text.substring(0, 3);
    }

    private String normalizeProcessCode(String processCode) {
        return StrUtil.blankToDefault(StrUtil.trim(processCode), "").toUpperCase(Locale.ROOT);
    }

    private String normalizeTargetType(String targetType, ProcessOption option) {
        if (option != null) {
            return option.targetType();
        }
        String normalized = StrUtil.blankToDefault(StrUtil.trim(targetType), "").toUpperCase(Locale.ROOT);
        if (TARGET_TYPE_OUTPUT_QTY.equals(normalized) || TARGET_TYPE_YIELD_RATE.equals(normalized)) {
            return normalized;
        }
        return TARGET_TYPE_OUTPUT_QTY;
    }

    private Integer normalizeSegmentCount(String modelCode, Integer segmentCount) {
        return 0;
    }

    private Integer defaultSegmentCount(Integer segmentCount) {
        return segmentCount == null ? 0 : segmentCount;
    }

    private String resolveMeasureUnit(String measureUnit, ProcessOption option) {
        if (option != null) {
            return option.unit();
        }
        return StrUtil.blankToDefault(StrUtil.trim(measureUnit), "");
    }

    private String targetTypeName(String targetType) {
        return TARGET_TYPE_YIELD_RATE.equals(targetType) ? "良品率百分比" : "产出值绝对值";
    }

    private String segmentCountName(String modelCode, Integer segmentCount) {
        return "基础配置";
    }

    private record ProcessOption(String name, String unit, String targetType) {
    }
}
