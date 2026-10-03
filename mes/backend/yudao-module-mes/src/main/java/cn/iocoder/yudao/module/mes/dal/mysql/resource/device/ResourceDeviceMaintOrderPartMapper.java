package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderPartDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintOrderPartMapper extends BaseMapperX<ResourceDeviceMaintOrderPartDO> {

    default List<ResourceDeviceMaintOrderPartDO> selectByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintOrderPartDO>()
                .eq(ResourceDeviceMaintOrderPartDO::getTaskId, taskId)
                .orderByAsc(ResourceDeviceMaintOrderPartDO::getSort)
                .orderByAsc(ResourceDeviceMaintOrderPartDO::getId));
    }

    default void deleteByTaskId(Long taskId) {
        delete(new LambdaQueryWrapperX<ResourceDeviceMaintOrderPartDO>().eq(ResourceDeviceMaintOrderPartDO::getTaskId, taskId));
    }

}
