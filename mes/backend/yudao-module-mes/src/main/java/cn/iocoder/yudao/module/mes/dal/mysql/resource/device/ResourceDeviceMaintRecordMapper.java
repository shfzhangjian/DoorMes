package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintRecordMapper extends BaseMapperX<ResourceDeviceMaintRecordDO> {

    default PageResult<ResourceDeviceMaintRecordDO> selectPage(ResourceDeviceMaintRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ResourceDeviceMaintRecordDO>()
                .eqIfPresent(ResourceDeviceMaintRecordDO::getRecordNo, reqVO.getRecordNo())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceMaintRecordDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getMaintType, reqVO.getMaintType())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getResultStatus, reqVO.getResultStatus())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getCategoryId, reqVO.getCategoryId())
                .betweenIfPresent(ResourceDeviceMaintRecordDO::getActualTime, reqVO.getActualTime())
                .orderByDesc(ResourceDeviceMaintRecordDO::getActualTime)
                .orderByDesc(ResourceDeviceMaintRecordDO::getId));
    }

    default ResourceDeviceMaintRecordDO selectByOrderId(Long orderId) {
        return selectOne(ResourceDeviceMaintRecordDO::getOrderId, orderId);
    }

    default java.util.List<ResourceDeviceMaintRecordDO> selectSummaryList(ResourceDeviceMaintRecordPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintRecordDO>()
                .eqIfPresent(ResourceDeviceMaintRecordDO::getRecordNo, reqVO.getRecordNo())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceMaintRecordDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getMaintType, reqVO.getMaintType())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getResultStatus, reqVO.getResultStatus())
                .eqIfPresent(ResourceDeviceMaintRecordDO::getCategoryId, reqVO.getCategoryId())
                .betweenIfPresent(ResourceDeviceMaintRecordDO::getActualTime, reqVO.getActualTime()));
    }

}
