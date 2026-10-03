package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDevicePartDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDevicePartMapper extends BaseMapperX<ResourceDevicePartDO> {

    default List<ResourceDevicePartDO> selectByDeviceId(Long deviceId) {
        return selectList(new LambdaQueryWrapperX<ResourceDevicePartDO>()
                .eq(ResourceDevicePartDO::getDeviceId, deviceId)
                .orderByAsc(ResourceDevicePartDO::getSort)
                .orderByAsc(ResourceDevicePartDO::getId));
    }

    default void deleteByDeviceId(Long deviceId) {
        delete(new LambdaQueryWrapperX<ResourceDevicePartDO>().eq(ResourceDevicePartDO::getDeviceId, deviceId));
    }

}
