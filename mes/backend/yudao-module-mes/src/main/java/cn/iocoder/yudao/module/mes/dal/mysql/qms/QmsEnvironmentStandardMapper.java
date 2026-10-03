package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEnvironmentStandardDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsEnvironmentStandardMapper extends BaseMapperX<QmsEnvironmentStandardDO> {

    default QmsEnvironmentStandardDO selectByWorkshop(String workshopCode, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsEnvironmentStandardDO>()
                .eq(QmsEnvironmentStandardDO::getWorkshopCode, workshopCode)
                .eqIfPresent(QmsEnvironmentStandardDO::getTenantId, tenantId)
                .last("LIMIT 1"));
    }

    default void updateByBusinessKey(QmsEnvironmentStandardDO standard) {
        update(null, new LambdaUpdateWrapper<QmsEnvironmentStandardDO>()
                .eq(QmsEnvironmentStandardDO::getId, standard.getId())
                .set(QmsEnvironmentStandardDO::getWorkshopCode, standard.getWorkshopCode())
                .set(QmsEnvironmentStandardDO::getWorkshopName, standard.getWorkshopName())
                .set(QmsEnvironmentStandardDO::getTemperatureMin, standard.getTemperatureMin())
                .set(QmsEnvironmentStandardDO::getTemperatureMax, standard.getTemperatureMax())
                .set(QmsEnvironmentStandardDO::getHumidityMin, standard.getHumidityMin())
                .set(QmsEnvironmentStandardDO::getHumidityMax, standard.getHumidityMax())
                .set(QmsEnvironmentStandardDO::getRemark, standard.getRemark())
                .set(QmsEnvironmentStandardDO::getTenantId, standard.getTenantId()));
    }
}
