package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmEvaluationTemplateVersionMapper extends BaseMapperX<SrmEvaluationTemplateVersionDO> {

    default List<SrmEvaluationTemplateVersionDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<SrmEvaluationTemplateVersionDO>()
                .eq(SrmEvaluationTemplateVersionDO::getTemplateId, templateId)
                .orderByDesc(SrmEvaluationTemplateVersionDO::getId));
    }

    default SrmEvaluationTemplateVersionDO selectByTemplateIdAndNo(Long templateId, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<SrmEvaluationTemplateVersionDO>()
                .eq(SrmEvaluationTemplateVersionDO::getTemplateId, templateId)
                .eq(SrmEvaluationTemplateVersionDO::getVersionNo, versionNo));
    }

}
