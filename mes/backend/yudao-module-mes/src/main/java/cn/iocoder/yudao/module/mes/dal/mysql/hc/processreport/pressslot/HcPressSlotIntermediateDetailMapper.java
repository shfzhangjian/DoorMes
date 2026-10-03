package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotIntermediateDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotIntermediateDetailMapper extends BaseMapperX<HcPressSlotIntermediateDetailDO> {

    default List<HcPressSlotIntermediateDetailDO> selectListByRecordId(Long intermediateRecordId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotIntermediateDetailDO>()
                .eq(HcPressSlotIntermediateDetailDO::getIntermediateRecordId, intermediateRecordId)
                .eq(HcPressSlotIntermediateDetailDO::getDeleted, false)
                .orderByAsc(HcPressSlotIntermediateDetailDO::getSortNo)
                .orderByAsc(HcPressSlotIntermediateDetailDO::getId));
    }

    default void deleteByRecordId(Long intermediateRecordId) {
        delete(new LambdaQueryWrapperX<HcPressSlotIntermediateDetailDO>()
                .eq(HcPressSlotIntermediateDetailDO::getIntermediateRecordId, intermediateRecordId));
    }
}
