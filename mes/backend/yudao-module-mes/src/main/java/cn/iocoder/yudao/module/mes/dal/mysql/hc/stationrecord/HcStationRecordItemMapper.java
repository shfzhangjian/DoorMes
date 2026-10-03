package cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcStationRecordItemMapper extends BaseMapperX<HcStationRecordItemDO> {

    default List<HcStationRecordItemDO> selectByRecordIds(Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcStationRecordItemDO>()
                .in(HcStationRecordItemDO::getRecordId, recordIds)
                .orderByAsc(HcStationRecordItemDO::getRecordId)
                .orderByAsc(HcStationRecordItemDO::getItemSeq)
                .orderByAsc(HcStationRecordItemDO::getId));
    }

    default void deleteByRecordId(Long recordId) {
        delete(new LambdaQueryWrapperX<HcStationRecordItemDO>()
                .eq(HcStationRecordItemDO::getRecordId, recordId));
    }

    @Delete("DELETE FROM mes_sfc_station_record_item WHERE record_id = #{recordId}")
    int physicalDeleteByRecordId(@Param("recordId") Long recordId);
}
