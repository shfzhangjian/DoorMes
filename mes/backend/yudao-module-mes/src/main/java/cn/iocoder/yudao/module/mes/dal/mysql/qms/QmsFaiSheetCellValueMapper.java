package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetCellValueDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSheetCellValueMapper extends BaseMapperX<QmsFaiSheetCellValueDO> {

    default List<QmsFaiSheetCellValueDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .eq(QmsFaiSheetCellValueDO::getFaiId, faiId)
                .orderByAsc(QmsFaiSheetCellValueDO::getSectionCode)
                .orderByAsc(QmsFaiSheetCellValueDO::getMetricCode)
                .orderByAsc(QmsFaiSheetCellValueDO::getRowNo)
                .orderByAsc(QmsFaiSheetCellValueDO::getColumnNo)
                .orderByAsc(QmsFaiSheetCellValueDO::getFieldCode));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .eq(QmsFaiSheetCellValueDO::getFaiId, faiId));
    }

    default int deleteByFaiIdAndItemIds(Long faiId, Collection<Long> faiItemIds) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .eq(QmsFaiSheetCellValueDO::getFaiId, faiId)
                .in(QmsFaiSheetCellValueDO::getFaiItemId, faiItemIds));
    }

    default int deleteByFaiIdAndItemIdAndDisplayLabels(Long faiId, Long faiItemId, Collection<String> displayLabels) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .eq(QmsFaiSheetCellValueDO::getFaiId, faiId)
                .eq(QmsFaiSheetCellValueDO::getFaiItemId, faiItemId)
                .in(QmsFaiSheetCellValueDO::getDisplayLabel, displayLabels));
    }

    default List<QmsFaiSheetCellValueDO> selectListByFaiIdAndItemIds(Long faiId, Collection<Long> faiItemIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .eq(QmsFaiSheetCellValueDO::getFaiId, faiId)
                .in(QmsFaiSheetCellValueDO::getFaiItemId, faiItemIds)
                .orderByAsc(QmsFaiSheetCellValueDO::getFaiItemId)
                .orderByAsc(QmsFaiSheetCellValueDO::getSectionCode)
                .orderByAsc(QmsFaiSheetCellValueDO::getMetricCode)
                .orderByAsc(QmsFaiSheetCellValueDO::getRowNo)
                .orderByAsc(QmsFaiSheetCellValueDO::getColumnNo)
                .orderByAsc(QmsFaiSheetCellValueDO::getFieldCode));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetCellValueDO>()
                .in(QmsFaiSheetCellValueDO::getFaiId, faiIds));
    }
}
