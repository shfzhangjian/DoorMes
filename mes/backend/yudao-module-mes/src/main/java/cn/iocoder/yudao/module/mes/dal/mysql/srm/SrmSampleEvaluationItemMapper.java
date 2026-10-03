package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationItemMapper extends BaseMapperX<SrmSampleEvaluationItemDO> {

    default List<SrmSampleEvaluationItemDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationItemDO>()
                .eq(SrmSampleEvaluationItemDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmSampleEvaluationItemDO::getRowNo)
                .orderByAsc(SrmSampleEvaluationItemDO::getSortNo)
                .orderByAsc(SrmSampleEvaluationItemDO::getId));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmSampleEvaluationItemDO::getEvaluationId, evaluationId);
    }

}
