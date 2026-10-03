package cn.iocoder.yudao.module.mes.dal.mysql.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintPlanDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceDeviceMaintPlanMapper extends BaseMapperX<ResourceDeviceMaintPlanDO> {

    default PageResult<ResourceDeviceMaintPlanDO> selectPage(ResourceDeviceMaintPlanPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByAsc(ResourceDeviceMaintPlanDO::getPlanYear)
                .orderByAsc(ResourceDeviceMaintPlanDO::getMonthNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getWeekNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getDeviceCode)
                .orderByAsc(ResourceDeviceMaintPlanDO::getId));
    }

    default List<ResourceDeviceMaintPlanDO> selectList(ResourceDeviceMaintPlanPageReqVO reqVO) {
        return selectList(buildQuery(reqVO)
                .orderByAsc(ResourceDeviceMaintPlanDO::getPlanYear)
                .orderByAsc(ResourceDeviceMaintPlanDO::getMonthNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getWeekNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getDeviceCode)
                .orderByAsc(ResourceDeviceMaintPlanDO::getId));
    }

    default List<ResourceDeviceMaintPlanDO> selectByYear(Integer planYear) {
        return selectByYear(planYear, null);
    }

    default List<ResourceDeviceMaintPlanDO> selectByYear(Integer planYear, Long deviceId) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintPlanDO>()
                .eq(ResourceDeviceMaintPlanDO::getPlanYear, planYear)
                .eqIfPresent(ResourceDeviceMaintPlanDO::getDeviceId, deviceId)
                .orderByAsc(ResourceDeviceMaintPlanDO::getDeviceCode)
                .orderByAsc(ResourceDeviceMaintPlanDO::getStandardId)
                .orderByAsc(ResourceDeviceMaintPlanDO::getStandardItemId)
                .orderByAsc(ResourceDeviceMaintPlanDO::getMonthNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getWeekNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getId));
    }

    default ResourceDeviceMaintPlanDO selectByItemMonth(Integer planYear, Long standardItemId, Integer monthNo) {
        return selectOne(new LambdaQueryWrapperX<ResourceDeviceMaintPlanDO>()
                .eq(ResourceDeviceMaintPlanDO::getPlanYear, planYear)
                .eq(ResourceDeviceMaintPlanDO::getStandardItemId, standardItemId)
                .eq(ResourceDeviceMaintPlanDO::getMonthNo, monthNo)
                .orderByAsc(ResourceDeviceMaintPlanDO::getId)
                .last("LIMIT 1"));
    }

    default List<ResourceDeviceMaintPlanDO> selectUnpublished(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<ResourceDeviceMaintPlanDO>()
                .in(ResourceDeviceMaintPlanDO::getId, ids)
                .eq(ResourceDeviceMaintPlanDO::getPublished, false)
                .orderByAsc(ResourceDeviceMaintPlanDO::getPlanDate)
                .orderByAsc(ResourceDeviceMaintPlanDO::getId));
    }

    default void deleteByYear(Integer planYear) {
        delete(new LambdaQueryWrapperX<ResourceDeviceMaintPlanDO>()
                .eq(ResourceDeviceMaintPlanDO::getPlanYear, planYear));
    }

    private LambdaQueryWrapperX<ResourceDeviceMaintPlanDO> buildQuery(ResourceDeviceMaintPlanPageReqVO reqVO) {
        return new LambdaQueryWrapperX<ResourceDeviceMaintPlanDO>()
                .eqIfPresent(ResourceDeviceMaintPlanDO::getPlanYear, reqVO.getPlanYear())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getDeviceCode, reqVO.getDeviceCode())
                .likeIfPresent(ResourceDeviceMaintPlanDO::getDeviceName, reqVO.getDeviceName())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getMaintType, reqVO.getMaintType())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getMonthNo, reqVO.getMonthNo())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getWeekNo, reqVO.getWeekNo())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getPublished, reqVO.getPublished())
                .eqIfPresent(ResourceDeviceMaintPlanDO::getStatus, reqVO.getStatus());
    }

}
