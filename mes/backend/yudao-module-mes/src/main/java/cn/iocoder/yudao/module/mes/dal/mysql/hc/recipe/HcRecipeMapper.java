package cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcRecipeMapper extends BaseMapperX<HcRecipeDO> {

    default PageResult<HcRecipeDO> selectPage(HcRecipePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcRecipeDO> selectList(HcRecipePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcRecipeDO> buildQuery(HcRecipePageReqVO reqVO) {
        LambdaQueryWrapperX<HcRecipeDO> query = new LambdaQueryWrapperX<HcRecipeDO>()
                .likeIfPresent(HcRecipeDO::getRecipeCode, reqVO.getRecipeCode())
                .likeIfPresent(HcRecipeDO::getRecipeName, reqVO.getRecipeName())
                .eqIfPresent(HcRecipeDO::getRecipeType, reqVO.getRecipeType())
                .likeIfPresent(HcRecipeDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(HcRecipeDO::getVersionNo, reqVO.getVersionNo())
                .eqIfPresent(HcRecipeDO::getSolidContentStd, reqVO.getSolidContentStd())
                .eqIfPresent(HcRecipeDO::getViscosityStd, reqVO.getViscosityStd())
                .eqIfPresent(HcRecipeDO::getYieldRate, reqVO.getYieldRate())
                .eqIfPresent(HcRecipeDO::getEffectiveDate, reqVO.getEffectiveDate())
                .eqIfPresent(HcRecipeDO::getExpireDate, reqVO.getExpireDate())
                .eqIfPresent(HcRecipeDO::getStatus, reqVO.getStatus())
                .likeIfPresent(HcRecipeDO::getRemark, reqVO.getRemark());
        query.orderByAsc(HcRecipeDO::getRecipeCode);
        query.orderByDesc(HcRecipeDO::getId);
        return query;
    }
}
