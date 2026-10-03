package cn.iocoder.yudao.module.mes.dal.mysql.hc.workcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcWorkCenterMapper extends BaseMapperX<HcWorkCenterDO> {

    default PageResult<HcWorkCenterDO> selectPage(HcWorkCenterPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcWorkCenterDO> selectList(HcWorkCenterPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcWorkCenterDO> buildQuery(HcWorkCenterPageReqVO reqVO) {
        LambdaQueryWrapperX<HcWorkCenterDO> queryWrapper = new LambdaQueryWrapperX<HcWorkCenterDO>()
                .likeIfPresent(HcWorkCenterDO::getWcCode, reqVO.getWcCode())
                .likeIfPresent(HcWorkCenterDO::getWcName, reqVO.getWcName())
                .likeIfPresent(HcWorkCenterDO::getProcessStage, reqVO.getProcessStage())
                .eqIfPresent(HcWorkCenterDO::getProcessId, reqVO.getProcessId())
                .likeIfPresent(HcWorkCenterDO::getProcessCode, reqVO.getProcessCode())
                .likeIfPresent(HcWorkCenterDO::getProcessName, reqVO.getProcessName())
                .eqIfPresent(HcWorkCenterDO::getLineCode, reqVO.getLineCode())
                .likeIfPresent(HcWorkCenterDO::getLineName, reqVO.getLineName())
                .likeIfPresent(HcWorkCenterDO::getTerminalIps, reqVO.getTerminalIps())
                .likeIfPresent(HcWorkCenterDO::getLineShortCode, reqVO.getLineShortCode())
                .likeIfPresent(HcWorkCenterDO::getBatchLineCode, reqVO.getBatchLineCode())
                .eqIfPresent(HcWorkCenterDO::getCapacityPerHour, reqVO.getCapacityPerHour())
                .eqIfPresent(HcWorkCenterDO::getCapacityUom, reqVO.getCapacityUom())
                .eqIfPresent(HcWorkCenterDO::getDefaultShiftMode, reqVO.getDefaultShiftMode())
                .eqIfPresent(HcWorkCenterDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcWorkCenterDO::getRemark, reqVO.getRemark());
        queryWrapper.orderByAsc(HcWorkCenterDO::getLineSort);
        queryWrapper.orderByAsc(HcWorkCenterDO::getLineCode);
        queryWrapper.orderByAsc(HcWorkCenterDO::getWcCode);
        queryWrapper.orderByDesc(HcWorkCenterDO::getId);
        return queryWrapper;
    }
}
