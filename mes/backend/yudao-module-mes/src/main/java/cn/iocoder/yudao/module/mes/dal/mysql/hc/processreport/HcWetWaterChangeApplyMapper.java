package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcWetWaterChangeApplyDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcWetWaterChangeApplyMapper extends BaseMapperX<HcWetWaterChangeApplyDO> {

    default HcWetWaterChangeApplyDO selectLatestUnexpired(LocalDate currentDate) {
        return selectOne(new LambdaQueryWrapperX<HcWetWaterChangeApplyDO>()
                .ge(HcWetWaterChangeApplyDO::getChangeEndDate, currentDate)
                .eq(HcWetWaterChangeApplyDO::getDeleted, false)
                .orderByDesc(HcWetWaterChangeApplyDO::getChangeStartDate)
                .orderByDesc(HcWetWaterChangeApplyDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcWetWaterChangeApplyDO> selectByDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<HcWetWaterChangeApplyDO>()
                .le(HcWetWaterChangeApplyDO::getChangeStartDate, endDate)
                .ge(HcWetWaterChangeApplyDO::getChangeEndDate, startDate)
                .eq(HcWetWaterChangeApplyDO::getDeleted, false)
                .orderByAsc(HcWetWaterChangeApplyDO::getChangeStartDate)
                .orderByAsc(HcWetWaterChangeApplyDO::getId));
    }
}
