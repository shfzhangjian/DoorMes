package cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductModelMapper extends BaseMapperX<HcProductModelDO> {

    default PageResult<HcProductModelDO> selectPage(HcProductModelPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcProductModelDO> selectList(HcProductModelPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcProductModelDO> buildQuery(HcProductModelPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcProductModelDO>()
                .likeIfPresent(HcProductModelDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcProductModelDO::getModelName, reqVO.getModelName())
                .likeIfPresent(HcProductModelDO::getModelRuleName, reqVO.getModelRuleName())
                .eqIfPresent(HcProductModelDO::getProdType, reqVO.getProdType())
                .eqIfPresent(HcProductModelDO::getCategoryCode, reqVO.getCategoryCode())
                .eqIfPresent(HcProductModelDO::getSizeSpec, reqVO.getSizeSpec())
                .eqIfPresent(HcProductModelDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcProductModelDO::getId);
    }

    default HcProductModelDO selectByModelCode(String modelCode) {
        return selectOne(HcProductModelDO::getModelCode, modelCode);
    }

}
