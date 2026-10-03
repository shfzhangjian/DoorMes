package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotCheckDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotCheckDetailMapper extends BaseMapperX<HcPressSlotCheckDetailDO> {

    default List<HcPressSlotCheckDetailDO> selectListByReportId(Long pressSlotReportId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotCheckDetailDO>()
                .eq(HcPressSlotCheckDetailDO::getPressSlotReportId, pressSlotReportId)
                .eq(HcPressSlotCheckDetailDO::getDeleted, false)
                .orderByAsc(HcPressSlotCheckDetailDO::getSortNo)
                .orderByAsc(HcPressSlotCheckDetailDO::getId));
    }

    default void deleteByReportId(Long pressSlotReportId) {
        delete(new LambdaQueryWrapperX<HcPressSlotCheckDetailDO>()
                .eq(HcPressSlotCheckDetailDO::getPressSlotReportId, pressSlotReportId));
    }

    default void deleteTemperatureItemsByReportId(Long pressSlotReportId) {
        delete(new LambdaQueryWrapperX<HcPressSlotCheckDetailDO>()
                .eq(HcPressSlotCheckDetailDO::getPressSlotReportId, pressSlotReportId)
                .like(HcPressSlotCheckDetailDO::getItemName, "温度点"));
    }

    default void deleteByReportIdAndCategory(Long pressSlotReportId, String itemCategory) {
        delete(new LambdaQueryWrapperX<HcPressSlotCheckDetailDO>()
                .eq(HcPressSlotCheckDetailDO::getPressSlotReportId, pressSlotReportId)
                .eq(HcPressSlotCheckDetailDO::getItemCategory, itemCategory));
    }
}
