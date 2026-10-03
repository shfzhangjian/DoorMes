package cn.iocoder.yudao.module.mes.dal.mysql.hc.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.team.HcTeamDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcTeamMapper extends BaseMapperX<HcTeamDO> {

    default PageResult<HcTeamDO> selectPage(HcTeamPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcTeamDO> selectList(HcTeamPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcTeamDO> buildQuery(HcTeamPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcTeamDO>()
                .likeIfPresent(HcTeamDO::getTeamCode, reqVO.getTeamCode())
                .likeIfPresent(HcTeamDO::getTeamName, reqVO.getTeamName())
                .eqIfPresent(HcTeamDO::getWorkCenterId, reqVO.getWorkCenterId())
                .likeIfPresent(HcTeamDO::getWorkCenterCode, reqVO.getWorkCenterCode())
                .eqIfPresent(HcTeamDO::getLeaderUserId, reqVO.getLeaderUserId())
                .likeIfPresent(HcTeamDO::getLeaderName, reqVO.getLeaderName())
                .eqIfPresent(HcTeamDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcTeamDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcTeamDO::getId);
    }
}