package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmEvaluationTemplateItemMapper extends BaseMapperX<SrmEvaluationTemplateItemDO> {

    default List<SrmEvaluationTemplateItemDO> selectListByVersionId(Long versionId) {
        return selectList(new LambdaQueryWrapperX<SrmEvaluationTemplateItemDO>()
                .eq(SrmEvaluationTemplateItemDO::getVersionId, versionId)
                .orderByAsc(SrmEvaluationTemplateItemDO::getGroupSort)
                .orderByAsc(SrmEvaluationTemplateItemDO::getIndicatorSort)
                .orderByAsc(SrmEvaluationTemplateItemDO::getId));
    }

    default void deleteByVersionId(Long versionId) {
        delete(SrmEvaluationTemplateItemDO::getVersionId, versionId);
    }

}
