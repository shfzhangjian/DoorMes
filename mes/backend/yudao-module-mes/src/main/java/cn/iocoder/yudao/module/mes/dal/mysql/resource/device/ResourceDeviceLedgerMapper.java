package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceLedgerDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceLedgerMapper extends BaseMapperX<ResourceDeviceLedgerDO> {

    default PageResult<ResourceDeviceLedgerDO> selectPage(ResourceDeviceLedgerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ResourceDeviceLedgerDO>()
                .eqIfPresent(ResourceDeviceLedgerDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceLedgerDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceLedgerDO::getCategoryId, reqVO.getCategoryIds() == null ? reqVO.getCategoryId() : null)
                .inIfPresent(ResourceDeviceLedgerDO::getCategoryId, reqVO.getCategoryIds())
                .eqIfPresent(ResourceDeviceLedgerDO::getDeviceType, reqVO.getDeviceType())
                .likeIfPresent(ResourceDeviceLedgerDO::getUsingDepartment, reqVO.getUsingDepartment())
                .eqIfPresent(ResourceDeviceLedgerDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ResourceDeviceLedgerDO::getLocation, reqVO.getLocation())
                .betweenIfPresent(ResourceDeviceLedgerDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ResourceDeviceLedgerDO::getId));
    }

    default ResourceDeviceLedgerDO selectByDeviceCode(String deviceCode) {
        return selectOne(ResourceDeviceLedgerDO::getDeviceCode, deviceCode);
    }

    default List<ResourceDeviceLedgerDO> selectEnabledList(List<Long> categoryIds, String deviceName) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceLedgerDO>()
                .eq(ResourceDeviceLedgerDO::getStatus, 1)
                .inIfPresent(ResourceDeviceLedgerDO::getCategoryId, categoryIds)
                .likeIfPresent(ResourceDeviceLedgerDO::getDeviceName, deviceName)
                .orderByAsc(ResourceDeviceLedgerDO::getCategoryId)
                .orderByAsc(ResourceDeviceLedgerDO::getDeviceCode)
                .orderByDesc(ResourceDeviceLedgerDO::getId));
    }

}
