package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceParamDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceParamMapper extends BaseMapperX<ResourceDeviceParamDO> {

    default List<ResourceDeviceParamDO> selectByDeviceId(Long deviceId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceParamDO>()
                .eq(ResourceDeviceParamDO::getDeviceId, deviceId)
                .orderByAsc(ResourceDeviceParamDO::getSort)
                .orderByAsc(ResourceDeviceParamDO::getId));
    }

    default void deleteByDeviceId(Long deviceId) {
        delete(new LambdaQueryWrapperX<ResourceDeviceParamDO>().eq(ResourceDeviceParamDO::getDeviceId, deviceId));
    }

}
