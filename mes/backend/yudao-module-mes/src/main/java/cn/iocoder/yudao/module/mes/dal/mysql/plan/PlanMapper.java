package cn.iocoder.yudao.module.mes.dal.mysql.plan;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.PlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.PlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlanMapper extends BaseMapperX<PlanDO> {

    default PageResult<PlanDO> selectPage(PlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PlanDO>()
                .likeIfPresent(PlanDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(PlanDO::getProductName, reqVO.getProductName())
                .eqIfPresent(PlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PlanDO::getFromSource, reqVO.getFromSource())
                .betweenIfPresent(PlanDO::getStartDate, reqVO.getStartDate())
                .orderByDesc(PlanDO::getId));
    }
}
