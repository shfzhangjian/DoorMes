package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcDispositionExecutionMapper extends BaseMapperX<QmsNcDispositionExecutionDO> {

    default QmsNcDispositionExecutionDO selectByNcRecordId(Long ncRecordId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcDispositionExecutionDO>()
                .eq(QmsNcDispositionExecutionDO::getNcRecordId, ncRecordId)
                .eq(QmsNcDispositionExecutionDO::getDeleted, false)
                .last("LIMIT 1"));
    }
}
