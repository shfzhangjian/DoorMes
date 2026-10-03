package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveIntermediateDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesiveIntermediateDetailMapper extends BaseMapperX<HcAdhesiveIntermediateDetailDO> {

    default List<HcAdhesiveIntermediateDetailDO> selectListByRecordId(Long intermediateRecordId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveIntermediateDetailDO>()
                .eq(HcAdhesiveIntermediateDetailDO::getIntermediateRecordId, intermediateRecordId)
                .eq(HcAdhesiveIntermediateDetailDO::getDeleted, false)
                .orderByAsc(HcAdhesiveIntermediateDetailDO::getSortNo)
                .orderByAsc(HcAdhesiveIntermediateDetailDO::getId));
    }

    default void deleteByRecordId(Long intermediateRecordId) {
        delete(new LambdaQueryWrapperX<HcAdhesiveIntermediateDetailDO>()
                .eq(HcAdhesiveIntermediateDetailDO::getIntermediateRecordId, intermediateRecordId));
    }
}
