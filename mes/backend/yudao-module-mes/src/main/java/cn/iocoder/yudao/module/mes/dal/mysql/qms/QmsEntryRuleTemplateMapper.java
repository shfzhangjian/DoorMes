package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEntryRuleTemplateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsEntryRuleTemplateMapper extends BaseMapperX<QmsEntryRuleTemplateDO> {

    default List<QmsEntryRuleTemplateDO> selectEnableList(String itemType) {
        return selectList(new LambdaQueryWrapperX<QmsEntryRuleTemplateDO>()
                .eq(QmsEntryRuleTemplateDO::getStatus, 0)
                .eqIfPresent(QmsEntryRuleTemplateDO::getItemType, itemType)
                .orderByDesc(QmsEntryRuleTemplateDO::getId));
    }
}
