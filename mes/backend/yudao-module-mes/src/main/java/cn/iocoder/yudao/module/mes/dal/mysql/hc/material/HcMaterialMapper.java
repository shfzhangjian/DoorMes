package cn.iocoder.yudao.module.mes.dal.mysql.hc.material;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcMaterialMapper extends BaseMapperX<HcMaterialDO> {

    default PageResult<HcMaterialDO> selectPage(HcMaterialPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcMaterialDO> selectList(HcMaterialPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcMaterialDO> buildQuery(HcMaterialPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcMaterialDO>()
                .likeIfPresent(HcMaterialDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcMaterialDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(HcMaterialDO::getMaterialShortName, reqVO.getMaterialShortName())
                .eqIfPresent(HcMaterialDO::getMaterialType, reqVO.getMaterialType())
                .eqIfPresent(HcMaterialDO::getMaterialCategoryId, reqVO.getMaterialCategoryId())
                .likeIfPresent(HcMaterialDO::getMaterialCategoryName, reqVO.getMaterialCategoryName())
                .eqIfPresent(HcMaterialDO::getProductLevel, reqVO.getProductLevel())
                .likeIfPresent(HcMaterialDO::getSpecModel, reqVO.getSpecModel())
                .eqIfPresent(HcMaterialDO::getMaterialCodeRuleId, reqVO.getMaterialCodeRuleId())
                .eqIfPresent(HcMaterialDO::getModelCodeRuleId, reqVO.getModelCodeRuleId())
                .likeIfPresent(HcMaterialDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(HcMaterialDO::getBaseUom, reqVO.getBaseUom())
                .eqIfPresent(HcMaterialDO::getStockUom, reqVO.getStockUom())
                .eqIfPresent(HcMaterialDO::getProduceUom, reqVO.getProduceUom())
                .eqIfPresent(HcMaterialDO::getBatchManaged, reqVO.getBatchManaged())
                .eqIfPresent(HcMaterialDO::getBarcodeManaged, reqVO.getBarcodeManaged())
                .eqIfPresent(HcMaterialDO::getDefaultRouteId, reqVO.getDefaultRouteId())
                .likeIfPresent(HcMaterialDO::getDefaultRouteCode, reqVO.getDefaultRouteCode())
                .likeIfPresent(HcMaterialDO::getDefaultRecipeCode, reqVO.getDefaultRecipeCode())
                .likeIfPresent(HcMaterialDO::getDefaultRecipeName, reqVO.getDefaultRecipeName())
                .eqIfPresent(HcMaterialDO::getDefaultBomId, reqVO.getDefaultBomId())
                .eqIfPresent(HcMaterialDO::getQualityControlMode, reqVO.getQualityControlMode())
                .eqIfPresent(HcMaterialDO::getMaterialStatus, reqVO.getMaterialStatus())
                .eqIfPresent(HcMaterialDO::getMesSelectVisible, reqVO.getMesSelectVisible())
                .eqIfPresent(HcMaterialDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcMaterialDO::getId);
    }
}
