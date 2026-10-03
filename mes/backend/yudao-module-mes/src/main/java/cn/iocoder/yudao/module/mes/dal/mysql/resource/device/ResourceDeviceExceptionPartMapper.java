package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionPartDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceExceptionPartMapper extends BaseMapperX<ResourceDeviceExceptionPartDO> {

    default List<ResourceDeviceExceptionPartDO> selectByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceExceptionPartDO>()
                .eq(ResourceDeviceExceptionPartDO::getExceptionId, exceptionId)
                .orderByAsc(ResourceDeviceExceptionPartDO::getSort)
                .orderByAsc(ResourceDeviceExceptionPartDO::getId));
    }

    default void deleteByExceptionId(Long exceptionId) {
        delete(new LambdaQueryWrapperX<ResourceDeviceExceptionPartDO>()
                .eq(ResourceDeviceExceptionPartDO::getExceptionId, exceptionId));
    }

}
