package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintOrderItemMapper extends BaseMapperX<ResourceDeviceMaintOrderItemDO> {

    default List<ResourceDeviceMaintOrderItemDO> selectByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintOrderItemDO>()
                .eq(ResourceDeviceMaintOrderItemDO::getTaskId, taskId)
                .orderByAsc(ResourceDeviceMaintOrderItemDO::getSort)
                .orderByAsc(ResourceDeviceMaintOrderItemDO::getId));
    }

    default void deleteByTaskId(Long taskId) {
        delete(new LambdaQueryWrapperX<ResourceDeviceMaintOrderItemDO>().eq(ResourceDeviceMaintOrderItemDO::getTaskId, taskId));
    }

    default void updatePendingResultByTaskId(Long taskId, String result) {
        ResourceDeviceMaintOrderItemDO updateObj = new ResourceDeviceMaintOrderItemDO();
        updateObj.setResult(result);
        update(updateObj, new LambdaQueryWrapperX<ResourceDeviceMaintOrderItemDO>()
                .eq(ResourceDeviceMaintOrderItemDO::getTaskId, taskId)
                .eq(ResourceDeviceMaintOrderItemDO::getResult, "PENDING"));
    }

}
