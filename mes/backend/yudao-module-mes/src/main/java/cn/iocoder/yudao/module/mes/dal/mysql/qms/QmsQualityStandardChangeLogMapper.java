package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardChangeLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsQualityStandardChangeLogMapper extends BaseMapperX<QmsQualityStandardChangeLogDO> {

    default List<QmsQualityStandardChangeLogDO> selectListByStandard(Long standardId, String applyType) {
        return selectList(new LambdaQueryWrapperX<QmsQualityStandardChangeLogDO>()
                .eq(QmsQualityStandardChangeLogDO::getStandardId, standardId)
                .eq(QmsQualityStandardChangeLogDO::getApplyType, applyType)
                .orderByDesc(QmsQualityStandardChangeLogDO::getChangeTime)
                .orderByDesc(QmsQualityStandardChangeLogDO::getId));
    }
}
