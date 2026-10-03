package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEnvironmentRecordDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsEnvironmentRecordMapper extends BaseMapperX<QmsEnvironmentRecordDO> {

    default List<QmsEnvironmentRecordDO> selectListByMonth(String workshopCode, LocalDate startDate,
                                                           LocalDate endExclusiveDate, Long tenantId) {
        return selectList(new LambdaQueryWrapperX<QmsEnvironmentRecordDO>()
                .eq(QmsEnvironmentRecordDO::getWorkshopCode, workshopCode)
                .eqIfPresent(QmsEnvironmentRecordDO::getTenantId, tenantId)
                .ge(QmsEnvironmentRecordDO::getRecordDate, startDate)
                .lt(QmsEnvironmentRecordDO::getRecordDate, endExclusiveDate)
                .orderByAsc(QmsEnvironmentRecordDO::getRecordDate)
                .orderByAsc(QmsEnvironmentRecordDO::getId));
    }

    default QmsEnvironmentRecordDO selectByWorkshopAndDate(String workshopCode, LocalDate recordDate, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsEnvironmentRecordDO>()
                .eq(QmsEnvironmentRecordDO::getWorkshopCode, workshopCode)
                .eq(QmsEnvironmentRecordDO::getRecordDate, recordDate)
                .eqIfPresent(QmsEnvironmentRecordDO::getTenantId, tenantId)
                .last("LIMIT 1"));
    }

    default void updateByRecordSave(QmsEnvironmentRecordDO record) {
        update(null, new LambdaUpdateWrapper<QmsEnvironmentRecordDO>()
                .eq(QmsEnvironmentRecordDO::getId, record.getId())
                .set(QmsEnvironmentRecordDO::getWorkshopCode, record.getWorkshopCode())
                .set(QmsEnvironmentRecordDO::getWorkshopName, record.getWorkshopName())
                .set(QmsEnvironmentRecordDO::getRecordMonth, record.getRecordMonth())
                .set(QmsEnvironmentRecordDO::getRecordDate, record.getRecordDate())
                .set(QmsEnvironmentRecordDO::getTemperatureValue, record.getTemperatureValue())
                .set(QmsEnvironmentRecordDO::getHumidityValue, record.getHumidityValue())
                .set(QmsEnvironmentRecordDO::getTemperatureStatus, record.getTemperatureStatus())
                .set(QmsEnvironmentRecordDO::getHumidityStatus, record.getHumidityStatus())
                .set(QmsEnvironmentRecordDO::getOverallStatus, record.getOverallStatus())
                .set(QmsEnvironmentRecordDO::getRecorderId, record.getRecorderId())
                .set(QmsEnvironmentRecordDO::getRecorderUsername, record.getRecorderUsername())
                .set(QmsEnvironmentRecordDO::getRecorderName, record.getRecorderName())
                .set(QmsEnvironmentRecordDO::getRecordTime, record.getRecordTime())
                .set(QmsEnvironmentRecordDO::getConfirmerId, record.getConfirmerId())
                .set(QmsEnvironmentRecordDO::getConfirmerUsername, record.getConfirmerUsername())
                .set(QmsEnvironmentRecordDO::getConfirmerName, record.getConfirmerName())
                .set(QmsEnvironmentRecordDO::getConfirmTime, record.getConfirmTime())
                .set(QmsEnvironmentRecordDO::getConfirmStatus, record.getConfirmStatus())
                .set(QmsEnvironmentRecordDO::getRemark, record.getRemark())
                .set(QmsEnvironmentRecordDO::getTenantId, record.getTenantId()));
    }

    default void updateConfirmInfo(QmsEnvironmentRecordDO record) {
        update(null, new LambdaUpdateWrapper<QmsEnvironmentRecordDO>()
                .eq(QmsEnvironmentRecordDO::getId, record.getId())
                .set(QmsEnvironmentRecordDO::getWorkshopName, record.getWorkshopName())
                .set(QmsEnvironmentRecordDO::getTemperatureStatus, record.getTemperatureStatus())
                .set(QmsEnvironmentRecordDO::getHumidityStatus, record.getHumidityStatus())
                .set(QmsEnvironmentRecordDO::getOverallStatus, record.getOverallStatus())
                .set(QmsEnvironmentRecordDO::getConfirmerId, record.getConfirmerId())
                .set(QmsEnvironmentRecordDO::getConfirmerUsername, record.getConfirmerUsername())
                .set(QmsEnvironmentRecordDO::getConfirmerName, record.getConfirmerName())
                .set(QmsEnvironmentRecordDO::getConfirmTime, record.getConfirmTime())
                .set(QmsEnvironmentRecordDO::getConfirmStatus, record.getConfirmStatus()));
    }
}
