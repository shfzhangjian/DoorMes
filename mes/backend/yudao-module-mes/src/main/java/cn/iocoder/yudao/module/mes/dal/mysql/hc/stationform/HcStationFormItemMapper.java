package cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcStationFormItemMapper extends BaseMapperX<HcStationFormItemDO> {

    default List<HcStationFormItemDO> selectByFormId(Long formId) {
        return selectList(new LambdaQueryWrapperX<HcStationFormItemDO>()
                .eq(HcStationFormItemDO::getFormId, formId)
                .orderByAsc(HcStationFormItemDO::getItemSeq)
                .orderByAsc(HcStationFormItemDO::getId));
    }

    default List<HcStationFormItemDO> selectByFormIds(Collection<Long> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcStationFormItemDO>()
                .in(HcStationFormItemDO::getFormId, formIds)
                .orderByAsc(HcStationFormItemDO::getFormId)
                .orderByAsc(HcStationFormItemDO::getItemSeq)
                .orderByAsc(HcStationFormItemDO::getId));
    }

    default void deleteByFormId(Long formId) {
        delete(new LambdaQueryWrapperX<HcStationFormItemDO>()
                .eq(HcStationFormItemDO::getFormId, formId));
    }

    default void deleteByFormIds(Collection<Long> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<HcStationFormItemDO>()
                .in(HcStationFormItemDO::getFormId, formIds));
    }
}
