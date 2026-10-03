package cn.iocoder.yudao.module.mes.dal.mysql.hc.fifopolicy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.fifopolicy.HcFifoPolicyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcFifoPolicyMapper extends BaseMapperX<HcFifoPolicyDO> {

    default PageResult<HcFifoPolicyDO> selectPage(HcFifoPolicyPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcFifoPolicyDO> selectList(HcFifoPolicyPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcFifoPolicyDO> buildQuery(HcFifoPolicyPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcFifoPolicyDO>()
                .likeIfPresent(HcFifoPolicyDO::getPolicyCode, reqVO.getPolicyCode())
                .likeIfPresent(HcFifoPolicyDO::getPolicyName, reqVO.getPolicyName())
                .likeIfPresent(HcFifoPolicyDO::getWarehouseCode, reqVO.getWarehouseCode())
                .likeIfPresent(HcFifoPolicyDO::getWarehouseName, reqVO.getWarehouseName())
                .likeIfPresent(HcFifoPolicyDO::getOwnerCode, reqVO.getOwnerCode())
                .eqIfPresent(HcFifoPolicyDO::getMatchScope, reqVO.getMatchScope())
                .eqIfPresent(HcFifoPolicyDO::getIssueRule, reqVO.getIssueRule())
                .eqIfPresent(HcFifoPolicyDO::getPriorityFields, reqVO.getPriorityFields())
                .eqIfPresent(HcFifoPolicyDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcFifoPolicyDO::getId);
    }
}