package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintStandardMapper extends BaseMapperX<ResourceDeviceMaintStandardDO> {

    default PageResult<ResourceDeviceMaintStandardDO> selectPage(ResourceDeviceMaintStandardPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ResourceDeviceMaintStandardDO>()
                .eqIfPresent(ResourceDeviceMaintStandardDO::getCode, reqVO.getCode())
                .likeIfPresent(ResourceDeviceMaintStandardDO::getName, reqVO.getName())
                .eqIfPresent(ResourceDeviceMaintStandardDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(ResourceDeviceMaintStandardDO::getDeviceType, reqVO.getDeviceType())
                .eqIfPresent(ResourceDeviceMaintStandardDO::getFrequency, reqVO.getFrequency())
                .eqIfPresent(ResourceDeviceMaintStandardDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ResourceDeviceMaintStandardDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ResourceDeviceMaintStandardDO::getId));
    }

    default ResourceDeviceMaintStandardDO selectByCode(String code) {
        return selectOne(ResourceDeviceMaintStandardDO::getCode, code);
    }

    default List<ResourceDeviceMaintStandardDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintStandardDO>()
                .eq(ResourceDeviceMaintStandardDO::getStatus, 1)
                .orderByAsc(ResourceDeviceMaintStandardDO::getCategoryId)
                .orderByAsc(ResourceDeviceMaintStandardDO::getCode)
                .orderByDesc(ResourceDeviceMaintStandardDO::getId));
    }

}
