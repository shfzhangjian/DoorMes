package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcReportGateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcReportGateMapper extends BaseMapperX<QmsNcReportGateDO> {

    default List<QmsNcReportGateDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<QmsNcReportGateDO>()
                .eq(QmsNcReportGateDO::getExecutionId, executionId)
                .eq(QmsNcReportGateDO::getDeleted, false)
                .orderByAsc(QmsNcReportGateDO::getId));
    }
}
