package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetCellValueDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSheetCellValueMapper extends BaseMapperX<QmsFqcSheetCellValueDO> {

    default List<QmsFqcSheetCellValueDO> selectListByFqcId(Long FqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcSheetCellValueDO>()
                .eq(QmsFqcSheetCellValueDO::getFqcId, FqcId)
                .orderByAsc(QmsFqcSheetCellValueDO::getSectionCode)
                .orderByAsc(QmsFqcSheetCellValueDO::getMetricCode)
                .orderByAsc(QmsFqcSheetCellValueDO::getRowNo)
                .orderByAsc(QmsFqcSheetCellValueDO::getColumnNo)
                .orderByAsc(QmsFqcSheetCellValueDO::getFieldCode));
    }

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetCellValueDO>()
                .eq(QmsFqcSheetCellValueDO::getFqcId, FqcId));
    }

    default int deleteByFqcIdAndItemIds(Long FqcId, Collection<Long> FqcItemIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetCellValueDO>()
                .eq(QmsFqcSheetCellValueDO::getFqcId, FqcId)
                .in(QmsFqcSheetCellValueDO::getFqcItemId, FqcItemIds));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetCellValueDO>()
                .in(QmsFqcSheetCellValueDO::getFqcId, FqcIds));
    }
}
