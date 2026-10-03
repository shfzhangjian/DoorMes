package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcWorkstationCommandDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcWorkstationCommandMapper extends BaseMapperX<QmsNcWorkstationCommandDO> {

    default QmsNcWorkstationCommandDO selectByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcWorkstationCommandDO>()
                .eq(QmsNcWorkstationCommandDO::getExecutionId, executionId)
                .eq(QmsNcWorkstationCommandDO::getDeleted, false)
                .orderByDesc(QmsNcWorkstationCommandDO::getCommandVersion)
                .last("LIMIT 1"));
    }
}
