package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceExceptionMapper extends BaseMapperX<ResourceDeviceExceptionDO> {

    default PageResult<ResourceDeviceExceptionDO> selectPage(ResourceDeviceExceptionPageReqVO reqVO) {
        LambdaQueryWrapperX<ResourceDeviceExceptionDO> wrapper = new LambdaQueryWrapperX<ResourceDeviceExceptionDO>()
                .eqIfPresent(ResourceDeviceExceptionDO::getExceptionNo, reqVO.getOrderNo())
                .eqIfPresent(ResourceDeviceExceptionDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceExceptionDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceExceptionDO::getExceptionLevel, reqVO.getExceptionLevel())
                .eqIfPresent(ResourceDeviceExceptionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ResourceDeviceExceptionDO::getReportTime, reqVO.getCreateTime());
        if ("todo".equals(reqVO.getTabType())) {
            Long currentUserId = reqVO.getCurrentUserId();
            if (currentUserId == null) {
                wrapper.in(ResourceDeviceExceptionDO::getStatus,
                        List.of("REPORTED", "DISPATCHED", "PENDING_CONFIRM", "PENDING_ARCHIVE"));
            } else {
                wrapper.and(query -> query.eq(ResourceDeviceExceptionDO::getStatus, "REPORTED")
                        .or(condition -> condition.eq(ResourceDeviceExceptionDO::getStatus, "DISPATCHED")
                                .eq(ResourceDeviceExceptionDO::getAssigneeId, currentUserId))
                        .or(condition -> condition.eq(ResourceDeviceExceptionDO::getStatus, "PENDING_CONFIRM")
                                .eq(ResourceDeviceExceptionDO::getReporterId, currentUserId))
                        .or(condition -> condition.eq(ResourceDeviceExceptionDO::getStatus, "PENDING_ARCHIVE")
                                .eq(ResourceDeviceExceptionDO::getDispatcherId, currentUserId)));
            }
        } else if ("initiated".equals(reqVO.getTabType())) {
            if (reqVO.getCurrentUserId() != null) {
                wrapper.eq(ResourceDeviceExceptionDO::getReporterId, reqVO.getCurrentUserId());
            } else {
                wrapper.ne(ResourceDeviceExceptionDO::getStatus, "CANCELLED");
            }
        }
        return selectPage(reqVO, wrapper.orderByDesc(ResourceDeviceExceptionDO::getReportTime)
                .orderByDesc(ResourceDeviceExceptionDO::getId));
    }

    default ResourceDeviceExceptionDO selectByExceptionNo(String exceptionNo) {
        return selectOne(ResourceDeviceExceptionDO::getExceptionNo, exceptionNo);
    }

}
