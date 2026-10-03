package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategoryListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceCategoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceCategoryMapper extends BaseMapperX<ResourceDeviceCategoryDO> {

    default List<ResourceDeviceCategoryDO> selectList(ResourceDeviceCategoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceCategoryDO>()
                .eqIfPresent(ResourceDeviceCategoryDO::getCategoryCode, reqVO.getCategoryCode())
                .likeIfPresent(ResourceDeviceCategoryDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(ResourceDeviceCategoryDO::getStatus, reqVO.getStatus())
                .orderByAsc(ResourceDeviceCategoryDO::getParentId)
                .orderByAsc(ResourceDeviceCategoryDO::getSort)
                .orderByDesc(ResourceDeviceCategoryDO::getId));
    }

    default ResourceDeviceCategoryDO selectByCode(String categoryCode) {
        return selectOne(ResourceDeviceCategoryDO::getCategoryCode, categoryCode);
    }

}
