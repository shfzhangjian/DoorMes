package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintStandardItemMapper extends BaseMapperX<ResourceDeviceMaintStandardItemDO> {

    default List<ResourceDeviceMaintStandardItemDO> selectByStandardId(Long standardId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintStandardItemDO>()
                .eq(ResourceDeviceMaintStandardItemDO::getStandardId, standardId)
                .orderByAsc(ResourceDeviceMaintStandardItemDO::getSort)
                .orderByAsc(ResourceDeviceMaintStandardItemDO::getId));
    }

    default void deleteByStandardId(Long standardId) {
        delete(new LambdaQueryWrapperX<ResourceDeviceMaintStandardItemDO>()
                .eq(ResourceDeviceMaintStandardItemDO::getStandardId, standardId));
    }

}
