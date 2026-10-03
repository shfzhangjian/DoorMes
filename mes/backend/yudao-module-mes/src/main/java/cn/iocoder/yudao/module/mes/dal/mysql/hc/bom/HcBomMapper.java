package cn.iocoder.yudao.module.mes.dal.mysql.hc.bom;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

@Mapper
public interface HcBomMapper extends BaseMapperX<HcBomDO> {

    default HcBomDO selectEnabledByMaterialCodeAndModelCode(String materialCode, String modelCode) {
        return selectOne(new LambdaQueryWrapperX<HcBomDO>()
                .eq(HcBomDO::getProductMaterialCode, materialCode)
                .eq(HcBomDO::getProductModelCode, modelCode)
                .eq(HcBomDO::getStatus, 1)
                .eq(HcBomDO::getDeleted, false)
                .orderByDesc(HcBomDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<HcBomDO> selectPage(HcBomPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcBomDO> selectList(HcBomPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcBomDO> buildQuery(HcBomPageReqVO reqVO) {
        LambdaQueryWrapperX<HcBomDO> queryWrapper = new LambdaQueryWrapperX<HcBomDO>()
                .likeIfPresent(HcBomDO::getBomCode, reqVO.getBomCode())
                .likeIfPresent(HcBomDO::getBomName, reqVO.getBomName())
                .eqIfPresent(HcBomDO::getProductMaterialId, reqVO.getProductMaterialId())
                .likeIfPresent(HcBomDO::getProductMaterialCode, reqVO.getProductMaterialCode())
                .likeIfPresent(HcBomDO::getProductMaterialName, reqVO.getProductMaterialName())
                .eqIfPresent(HcBomDO::getProductModelId, reqVO.getProductModelId())
                .likeIfPresent(HcBomDO::getProductModelCode, reqVO.getProductModelCode())
                .likeIfPresent(HcBomDO::getProductModelName, reqVO.getProductModelName())
                .likeIfPresent(HcBomDO::getProductSpec, reqVO.getProductSpec())
                .eqIfPresent(HcBomDO::getRecipeId, reqVO.getRecipeId())
                .likeIfPresent(HcBomDO::getRecipeCode, reqVO.getRecipeCode())
                .likeIfPresent(HcBomDO::getRecipeName, reqVO.getRecipeName())
                .eqIfPresent(HcBomDO::getVersionNo, reqVO.getVersionNo())
                .eqIfPresent(HcBomDO::getBomType, reqVO.getBomType())
                .eqIfPresent(HcBomDO::getYieldRate, reqVO.getYieldRate())
                .eqIfPresent(HcBomDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcBomDO::getRemark, reqVO.getRemark());
        if (StringUtils.hasText(reqVO.getProductMaterialKeyword())) {
            String keyword = reqVO.getProductMaterialKeyword().trim();
            queryWrapper.and(wrapper -> wrapper.like(HcBomDO::getProductMaterialCode, keyword)
                    .or()
                    .like(HcBomDO::getProductMaterialName, keyword));
        }
        if (StringUtils.hasText(reqVO.getRecipeKeyword())) {
            String keyword = reqVO.getRecipeKeyword().trim();
            queryWrapper.and(wrapper -> wrapper.like(HcBomDO::getRecipeCode, keyword)
                    .or()
                    .like(HcBomDO::getRecipeName, keyword));
        }
        return queryWrapper.orderByDesc(HcBomDO::getId);
    }
}
