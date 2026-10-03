package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEntryRuleTemplateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsEntryRuleTemplateMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ENTRY_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ENTRY_RULE_TEMPLATE_NOT_EXISTS;

@Service
@Validated
public class QmsEntryRuleTemplateServiceImpl implements QmsEntryRuleTemplateService {

    @Resource
    private QmsEntryRuleTemplateMapper qmsEntryRuleTemplateMapper;

    @Override
    public Long createEntryRuleTemplate(QmsEntryRuleTemplateSaveReqVO createReqVO) {
        validateTemplateParams(createReqVO.getTemplateParams());
        QmsEntryRuleTemplateDO entity = BeanUtils.toBean(createReqVO, QmsEntryRuleTemplateDO.class);
        entity.setId(null);
        entity.setStatus(0);
        qmsEntryRuleTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateEntryRuleTemplate(QmsEntryRuleTemplateSaveReqVO updateReqVO) {
        QmsEntryRuleTemplateDO existing = validateTemplateExists(updateReqVO.getId());
        validateTemplateParams(updateReqVO.getTemplateParams());
        QmsEntryRuleTemplateDO updateObj = BeanUtils.toBean(updateReqVO, QmsEntryRuleTemplateDO.class);
        updateObj.setStatus(existing.getStatus());
        qmsEntryRuleTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteEntryRuleTemplate(Long id) {
        validateTemplateExists(id);
        qmsEntryRuleTemplateMapper.updateById(QmsEntryRuleTemplateDO.builder()
                .id(id)
                .status(1)
                .build());
    }

    @Override
    public List<QmsEntryRuleTemplateRespVO> getEntryRuleTemplateList(String itemType) {
        return BeanUtils.toBean(qmsEntryRuleTemplateMapper.selectEnableList(itemType), QmsEntryRuleTemplateRespVO.class);
    }

    private void validateTemplateParams(String templateParams) {
        try {
            QmsEntryRuleFormulaSupport.validateTemplateParams(templateParams);
        } catch (IllegalArgumentException ex) {
            throw exception(HCQUALITYSTANDARD_ENTRY_RULE_INVALID);
        }
    }

    private QmsEntryRuleTemplateDO validateTemplateExists(Long id) {
        if (id == null) {
            throw exception(HCQUALITYSTANDARD_ENTRY_RULE_TEMPLATE_NOT_EXISTS);
        }
        QmsEntryRuleTemplateDO template = qmsEntryRuleTemplateMapper.selectById(id);
        if (template == null || !Objects.equals(template.getStatus(), 0)) {
            throw exception(HCQUALITYSTANDARD_ENTRY_RULE_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }
}
