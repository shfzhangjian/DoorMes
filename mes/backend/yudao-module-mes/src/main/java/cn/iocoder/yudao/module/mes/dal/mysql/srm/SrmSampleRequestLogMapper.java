package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleRequestLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleRequestLogMapper extends BaseMapperX<SrmSampleRequestLogDO> {

    default List<SrmSampleRequestLogDO> selectListByRequestId(Long requestId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleRequestLogDO>()
                .eq(SrmSampleRequestLogDO::getRequestId, requestId)
                .orderByDesc(SrmSampleRequestLogDO::getCreateTime)
                .orderByDesc(SrmSampleRequestLogDO::getId));
    }

}
