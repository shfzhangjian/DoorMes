package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintOrderMapper extends BaseMapperX<ResourceDeviceMaintOrderDO> {

    default PageResult<ResourceDeviceMaintOrderDO> selectPage(ResourceDeviceMaintOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<ResourceDeviceMaintOrderDO> wrapper = new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .eqIfPresent(ResourceDeviceMaintOrderDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceMaintOrderDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getMaintType, reqVO.getMaintType())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ResourceDeviceMaintOrderDO::getPlanDate, reqVO.getPlanDate())
                .betweenIfPresent(ResourceDeviceMaintOrderDO::getActualDate, reqVO.getActualDate());
        if ("todo".equals(reqVO.getTabType())) {
            wrapper.in(ResourceDeviceMaintOrderDO::getStatus, List.of("WAIT_DISPATCH", "WAIT_EXECUTE", "EXECUTING", "WAIT_CONFIRM", "OVERDUE"));
        } else if ("done".equals(reqVO.getTabType())) {
            wrapper.in(ResourceDeviceMaintOrderDO::getStatus, List.of("DONE", "ABNORMAL"));
        }
        return selectPage(reqVO, wrapper.orderByAsc(ResourceDeviceMaintOrderDO::getPlanDate)
                .orderByDesc(ResourceDeviceMaintOrderDO::getId));
    }

    default ResourceDeviceMaintOrderDO selectByTaskNo(String taskNo) {
        return selectOne(ResourceDeviceMaintOrderDO::getTaskNo, taskNo);
    }

    default List<ResourceDeviceMaintOrderDO> selectCurrentMonthOrders(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .ge(ResourceDeviceMaintOrderDO::getPlanDate, startDate)
                .le(ResourceDeviceMaintOrderDO::getPlanDate, endDate)
                .orderByAsc(ResourceDeviceMaintOrderDO::getPlanDate)
                .orderByDesc(ResourceDeviceMaintOrderDO::getId));
    }

    default ResourceDeviceMaintOrderDO selectCurrentMonthOrder(Long deviceId, Long standardId,
                                                               LocalDate startDate, LocalDate endDate) {
        return selectOne(new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .eq(ResourceDeviceMaintOrderDO::getDeviceId, deviceId)
                .eq(ResourceDeviceMaintOrderDO::getStandardId, standardId)
                .ne(ResourceDeviceMaintOrderDO::getStatus, "CANCELLED")
                .ge(ResourceDeviceMaintOrderDO::getPlanDate, startDate)
                .le(ResourceDeviceMaintOrderDO::getPlanDate, endDate)
                .last("LIMIT 1"));
    }

    default ResourceDeviceMaintOrderDO selectLatestByDeviceStandard(Long deviceId, Long standardId) {
        return selectOne(new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .eq(ResourceDeviceMaintOrderDO::getDeviceId, deviceId)
                .eq(ResourceDeviceMaintOrderDO::getStandardId, standardId)
                .ne(ResourceDeviceMaintOrderDO::getStatus, "CANCELLED")
                .orderByDesc(ResourceDeviceMaintOrderDO::getPlanDate)
                .orderByDesc(ResourceDeviceMaintOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<ResourceDeviceMaintOrderDO> selectDueOpenOrders(LocalDate planDate) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .in(ResourceDeviceMaintOrderDO::getStatus, List.of("WAIT_DISPATCH", "WAIT_EXECUTE", "EXECUTING"))
                .and(wrapper -> wrapper.lt(ResourceDeviceMaintOrderDO::getDueDate, planDate)
                        .or()
                        .isNull(ResourceDeviceMaintOrderDO::getDueDate)
                        .lt(ResourceDeviceMaintOrderDO::getPlanDate, planDate)));
    }

    default List<ResourceDeviceMaintOrderDO> selectSummaryList(ResourceDeviceMaintRecordPageReqVO reqVO,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintOrderDO>()
                .eqIfPresent(ResourceDeviceMaintOrderDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceMaintOrderDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getMaintType, reqVO.getMaintType())
                .eqIfPresent(ResourceDeviceMaintOrderDO::getCategoryId, reqVO.getCategoryId())
                .ge(ResourceDeviceMaintOrderDO::getPlanDate, startDate)
                .le(ResourceDeviceMaintOrderDO::getPlanDate, endDate));
    }

}
