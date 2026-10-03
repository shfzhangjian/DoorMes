package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcReviewConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcReviewConfigMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsNcReviewConfigServiceImpl implements QmsNcReviewConfigService {

    private static final ErrorCode QMS_NCR_REVIEW_CONFIG_NOT_EXISTS =
            new ErrorCode(1008100150, "NCR评审会签配置不存在");

    @Resource
    private QmsNcReviewConfigMapper qmsNcReviewConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReviewConfig(QmsNcReviewConfigSaveReqVO createReqVO) {
        QmsNcReviewConfigDO entity = toDO(createReqVO);
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
        entity.setSort(entity.getSort() == null ? 0 : entity.getSort());
        qmsNcReviewConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewConfig(QmsNcReviewConfigSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        QmsNcReviewConfigDO updateObj = toDO(updateReqVO);
        updateObj.setId(updateReqVO.getId());
        qmsNcReviewConfigMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewConfig(Long id) {
        validateExists(id);
        qmsNcReviewConfigMapper.deleteById(id);
    }

    @Override
    public QmsNcReviewConfigRespVO getReviewConfig(Long id) {
        return toResp(validateExists(id));
    }

    @Override
    public PageResult<QmsNcReviewConfigRespVO> getReviewConfigPage(QmsNcReviewConfigPageReqVO pageReqVO) {
        PageResult<QmsNcReviewConfigDO> pageResult = qmsNcReviewConfigMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toResp).collect(Collectors.toList()),
                pageResult.getTotal());
    }

    @Override
    public List<QmsNcReviewConfigRespVO> getSimpleList() {
        return qmsNcReviewConfigMapper.selectEnabledList().stream()
                .map(this::toResp).collect(Collectors.toList());
    }

    @Override
    public QmsNcReviewConfigRespVO getFinalApproverConfig() {
        QmsNcReviewConfigDO entity = qmsNcReviewConfigMapper.selectEnabledFinalApprover();
        return entity == null ? null : toResp(entity);
    }

    @Override
    public Map<String, QmsNcReviewConfigRespVO> getEnabledConfigMap(Collection<String> unitNames) {
        if (CollUtil.isEmpty(unitNames)) {
            return new LinkedHashMap<>();
        }
        List<String> names = unitNames.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim)
                .distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(names)) {
            return new LinkedHashMap<>();
        }
        return qmsNcReviewConfigMapper.selectEnabledListByUnitNames(names).stream()
                .map(this::toResp)
                .collect(Collectors.toMap(QmsNcReviewConfigRespVO::getUnitName, item -> item,
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    private QmsNcReviewConfigDO validateExists(Long id) {
        QmsNcReviewConfigDO entity = qmsNcReviewConfigMapper.selectById(id);
        if (entity == null) {
            throw exception(QMS_NCR_REVIEW_CONFIG_NOT_EXISTS);
        }
        return entity;
    }

    private QmsNcReviewConfigDO toDO(QmsNcReviewConfigSaveReqVO reqVO) {
        QmsNcReviewConfigDO entity = new QmsNcReviewConfigDO();
        entity.setUnitCode(StrUtil.trim(reqVO.getUnitCode()));
        entity.setUnitName(StrUtil.trim(reqVO.getUnitName()));
        entity.setDeptId(reqVO.getDeptId());
        entity.setDeptName(StrUtil.trim(reqVO.getDeptName()));
        entity.setHandlerUserIds(joinLongs(reqVO.getHandlerUserIds()));
        entity.setHandlerUserNames(joinStrings(reqVO.getHandlerUserNames()));
        entity.setStatus(reqVO.getStatus());
        entity.setSort(reqVO.getSort());
        entity.setRemark(StrUtil.trim(reqVO.getRemark()));
        entity.setOpinionTemplateJson(StrUtil.trim(reqVO.getOpinionTemplateJson()));
        return entity;
    }

    private QmsNcReviewConfigRespVO toResp(QmsNcReviewConfigDO entity) {
        QmsNcReviewConfigRespVO respVO = new QmsNcReviewConfigRespVO();
        respVO.setId(entity.getId());
        respVO.setUnitCode(entity.getUnitCode());
        respVO.setUnitName(entity.getUnitName());
        respVO.setDeptId(entity.getDeptId());
        respVO.setDeptName(entity.getDeptName());
        respVO.setHandlerUserIds(parseLongs(entity.getHandlerUserIds()));
        respVO.setHandlerUserNames(parseStrings(entity.getHandlerUserNames()));
        respVO.setStatus(entity.getStatus());
        respVO.setSort(entity.getSort());
        respVO.setRemark(entity.getRemark());
        respVO.setOpinionTemplateJson(entity.getOpinionTemplateJson());
        respVO.setCreateTime(entity.getCreateTime());
        return respVO;
    }

    private String joinLongs(List<Long> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    private String joinStrings(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim).collect(Collectors.joining(","));
    }

    private List<Long> parseLongs(String value) {
        List<Long> result = new ArrayList<>();
        for (String item : StrUtil.splitTrim(StrUtil.blankToDefault(value, ""), ',')) {
            if (StrUtil.isNotBlank(item)) {
                result.add(Long.valueOf(item));
            }
        }
        return result;
    }

    private List<String> parseStrings(String value) {
        return StrUtil.splitTrim(StrUtil.blankToDefault(value, ""), ',').stream()
                .filter(StrUtil::isNotBlank).collect(Collectors.toList());
    }
}
