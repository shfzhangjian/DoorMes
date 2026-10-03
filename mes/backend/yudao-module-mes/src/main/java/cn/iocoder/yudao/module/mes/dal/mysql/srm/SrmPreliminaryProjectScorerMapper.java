package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectScorerDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryProjectScorerMapper extends BaseMapperX<SrmPreliminaryProjectScorerDO> {

    default List<SrmPreliminaryProjectScorerDO> selectListByProjectAndTemplateVersion(Long projectId,
                                                                                       Long templateVersionId) {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryProjectScorerDO>()
                .eq(SrmPreliminaryProjectScorerDO::getProjectId, projectId)
                .eq(SrmPreliminaryProjectScorerDO::getTemplateVersionId, templateVersionId)
                .orderByAsc(SrmPreliminaryProjectScorerDO::getGroupSort)
                .orderByAsc(SrmPreliminaryProjectScorerDO::getIndicatorSort)
                .orderByAsc(SrmPreliminaryProjectScorerDO::getId));
    }

    default SrmPreliminaryProjectScorerDO selectByProjectAndTemplateItem(Long projectId, Long templateItemId) {
        return selectOne(new LambdaQueryWrapperX<SrmPreliminaryProjectScorerDO>()
                .eq(SrmPreliminaryProjectScorerDO::getProjectId, projectId)
                .eq(SrmPreliminaryProjectScorerDO::getTemplateItemId, templateItemId));
    }

    default void deleteByProjectAndTemplateVersion(Long projectId, Long templateVersionId) {
        delete(new LambdaQueryWrapperX<SrmPreliminaryProjectScorerDO>()
                .eq(SrmPreliminaryProjectScorerDO::getProjectId, projectId)
                .eq(SrmPreliminaryProjectScorerDO::getTemplateVersionId, templateVersionId));
    }

}
