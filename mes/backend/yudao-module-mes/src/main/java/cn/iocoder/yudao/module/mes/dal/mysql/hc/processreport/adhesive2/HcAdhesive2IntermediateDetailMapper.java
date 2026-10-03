package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2IntermediateDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesive2IntermediateDetailMapper extends BaseMapperX<HcAdhesive2IntermediateDetailDO> {

    default List<HcAdhesive2IntermediateDetailDO> selectListByRecordId(Long intermediateRecordId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2IntermediateDetailDO>()
                .eq(HcAdhesive2IntermediateDetailDO::getIntermediateRecordId, intermediateRecordId)
                .eq(HcAdhesive2IntermediateDetailDO::getDeleted, false)
                .orderByAsc(HcAdhesive2IntermediateDetailDO::getSortNo)
                .orderByAsc(HcAdhesive2IntermediateDetailDO::getId));
    }

    default void deleteByRecordId(Long intermediateRecordId) {
        delete(new LambdaQueryWrapperX<HcAdhesive2IntermediateDetailDO>()
                .eq(HcAdhesive2IntermediateDetailDO::getIntermediateRecordId, intermediateRecordId));
    }
}
