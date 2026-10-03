package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmEvaluationTemplateLogMapper extends BaseMapperX<SrmEvaluationTemplateLogDO> {

    default List<SrmEvaluationTemplateLogDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<SrmEvaluationTemplateLogDO>()
                .eq(SrmEvaluationTemplateLogDO::getTemplateId, templateId)
                .orderByDesc(SrmEvaluationTemplateLogDO::getCreateTime)
                .orderByDesc(SrmEvaluationTemplateLogDO::getId));
    }

}
