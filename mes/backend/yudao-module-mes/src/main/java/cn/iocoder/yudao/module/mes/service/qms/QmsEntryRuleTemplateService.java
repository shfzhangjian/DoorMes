package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEntryRuleTemplateSaveReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface QmsEntryRuleTemplateService {

    Long createEntryRuleTemplate(@Valid QmsEntryRuleTemplateSaveReqVO createReqVO);

    void updateEntryRuleTemplate(@Valid QmsEntryRuleTemplateSaveReqVO updateReqVO);

    void deleteEntryRuleTemplate(Long id);

    List<QmsEntryRuleTemplateRespVO> getEntryRuleTemplateList(String itemType);
}
