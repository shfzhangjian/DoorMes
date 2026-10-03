package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCauseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCauseMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCodeMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCAUSE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCAUSE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCODE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCODE_HAS_CHILDREN;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCDEFECTCODE_PARENT_INVALID;

@Service
@Validated
public class QmsDefectCodeServiceImpl implements QmsDefectCodeService {

    private static final String TYPE_CATEGORY = "CATEGORY";
    private static final String TYPE_ITEM = "ITEM";

    @Resource
    private QmsDefectCodeMapper qmsDefectCodeMapper;
    @Resource
    private QmsDefectCauseMapper qmsDefectCauseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDefectCode(QmsDefectCodeSaveReqVO createReqVO) {
        validateCodeUnique(null, createReqVO.getCode());
        validateParent(createReqVO.getParentId(), null);
        QmsDefectCodeDO entity = BeanUtils.toBean(createReqVO, QmsDefectCodeDO.class);
        normalizeEntity(entity, null);
        qmsDefectCodeMapper.insert(entity);
        saveCauses(entity, createReqVO.getCauses());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefectCode(QmsDefectCodeSaveReqVO updateReqVO) {
        QmsDefectCodeDO oldEntity = validateDefectCodeExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        validateParent(updateReqVO.getParentId(), updateReqVO.getId());
        QmsDefectCodeDO updateObj = BeanUtils.toBean(updateReqVO, QmsDefectCodeDO.class);
        normalizeEntity(updateObj, oldEntity);
        qmsDefectCodeMapper.updateById(updateObj);
        qmsDefectCauseMapper.deleteByDefectCodeId(updateReqVO.getId());
        saveCauses(updateObj, updateReqVO.getCauses());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefectCode(Long id) {
        validateDefectCodeExists(id);
        if (!qmsDefectCodeMapper.selectListByParentId(id).isEmpty()) {
            throw exception(HCDEFECTCODE_HAS_CHILDREN);
        }
        qmsDefectCodeMapper.deleteById(id);
        qmsDefectCauseMapper.deleteByDefectCodeId(id);
    }

    @Override
    public QmsDefectCodeDO getDefectCode(Long id) {
        return validateDefectCodeExists(id);
    }

    @Override
    public QmsDefectCodeRespVO getDefectCodeResp(Long id) {
        QmsDefectCodeDO entity = validateDefectCodeExists(id);
        QmsDefectCodeRespVO respVO = BeanUtils.toBean(entity, QmsDefectCodeRespVO.class);
        respVO.setCauses(BeanUtils.toBean(
                qmsDefectCauseMapper.selectListByDefectCodeId(id),
                QmsDefectCodeRespVO.Cause.class));
        return respVO;
    }

    @Override
    public List<QmsDefectCodeDO> getDefectCodeList(QmsDefectCodeListReqVO reqVO) {
        return qmsDefectCodeMapper.selectList(reqVO);
    }

    private QmsDefectCodeDO validateDefectCodeExists(Long id) {
        QmsDefectCodeDO entity = qmsDefectCodeMapper.selectById(id);
        if (entity == null) {
            throw exception(HCDEFECTCODE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateCodeUnique(Long id, String code) {
        QmsDefectCodeDO entity = qmsDefectCodeMapper.selectOne(new LambdaQueryWrapperX<QmsDefectCodeDO>()
                .eq(QmsDefectCodeDO::getCode, code)
                .neIfPresent(QmsDefectCodeDO::getId, id));
        if (entity != null) {
            throw exception(HCDEFECTCODE_CODE_EXISTS);
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw exception(HCDEFECTCODE_PARENT_INVALID);
        }
        QmsDefectCodeDO parent = validateDefectCodeExists(parentId);
        if (!TYPE_CATEGORY.equals(parent.getType())) {
            throw exception(HCDEFECTCODE_PARENT_INVALID);
        }
        while (parent.getParentId() != null && parent.getParentId() > 0) {
            if (selfId != null && Objects.equals(parent.getParentId(), selfId)) {
                throw exception(HCDEFECTCODE_PARENT_INVALID);
            }
            parent = validateDefectCodeExists(parent.getParentId());
        }
    }

    private void normalizeEntity(QmsDefectCodeDO entity, QmsDefectCodeDO oldEntity) {
        if (entity.getSort() == null || entity.getSort() <= 0) {
            entity.setSort(oldEntity != null && oldEntity.getSort() != null
                    ? oldEntity.getSort()
                    : qmsDefectCodeMapper.selectMaxSortByParentId(entity.getParentId()) + 10);
        }
        if (TYPE_CATEGORY.equals(entity.getType())) {
            entity.setLevel(null);
            entity.setReferencePicUrls(null);
            return;
        }
        if (!TYPE_ITEM.equals(entity.getType()) || entity.getLevel() == null || entity.getLevel().isBlank()) {
            throw exception(HCDEFECTCODE_PARENT_INVALID);
        }
    }

    private void saveCauses(QmsDefectCodeDO defectCode, List<QmsDefectCodeSaveReqVO.Cause> causes) {
        if (!TYPE_ITEM.equals(defectCode.getType()) || causes == null || causes.isEmpty()) {
            return;
        }
        validateCauseCodesUnique(causes);

        List<QmsDefectCauseDO> causeList = new ArrayList<>();
        for (int i = 0; i < causes.size(); i++) {
            QmsDefectCodeSaveReqVO.Cause cause = causes.get(i);
            if (!StringUtils.hasText(cause.getReasonName())) {
                throw exception(HCDEFECTCAUSE_INVALID);
            }
            QmsDefectCauseDO causeDO = BeanUtils.toBean(cause, QmsDefectCauseDO.class);
            causeDO.setId(null);
            causeDO.setDefectCodeId(defectCode.getId());
            causeDO.setDefectCode(defectCode.getCode());
            causeDO.setDefectName(defectCode.getName());
            causeDO.setReasonCode(StringUtils.hasText(cause.getReasonCode()) ? cause.getReasonCode().trim() : null);
            causeDO.setSort(cause.getSort() != null && cause.getSort() > 0 ? cause.getSort() : (i + 1) * 10);
            causeDO.setStatus(cause.getStatus() == null ? 1 : cause.getStatus());
            causeDO.setTenantId(defectCode.getTenantId());
            causeList.add(causeDO);
        }
        if (!causeList.isEmpty()) {
            qmsDefectCauseMapper.insertBatch(causeList);
        }
    }

    private void validateCauseCodesUnique(List<QmsDefectCodeSaveReqVO.Cause> causes) {
        Set<String> reasonCodes = new HashSet<>();
        for (QmsDefectCodeSaveReqVO.Cause cause : causes) {
            if (!StringUtils.hasText(cause.getReasonCode())) {
                continue;
            }
            if (!reasonCodes.add(cause.getReasonCode().trim())) {
                throw exception(HCDEFECTCAUSE_CODE_EXISTS);
            }
        }
    }
}
