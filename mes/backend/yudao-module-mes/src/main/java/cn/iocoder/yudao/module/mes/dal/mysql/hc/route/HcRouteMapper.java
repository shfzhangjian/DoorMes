package cn.iocoder.yudao.module.mes.dal.mysql.hc.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcRouteMapper extends BaseMapperX<HcRouteDO> {

    default PageResult<HcRouteDO> selectPage(HcRoutePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcRouteDO> selectList(HcRoutePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcRouteDO> buildQuery(HcRoutePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcRouteDO>()
                .likeIfPresent(HcRouteDO::getRouteCode, reqVO.getRouteCode())
                .likeIfPresent(HcRouteDO::getRouteName, reqVO.getRouteName())
                .eqIfPresent(HcRouteDO::getApplicableScope, reqVO.getApplicableScope())
                .eqIfPresent(HcRouteDO::getProductMaterialId, reqVO.getProductMaterialId())
                .likeIfPresent(HcRouteDO::getProductMaterialCode, reqVO.getProductMaterialCode())
                .eqIfPresent(HcRouteDO::getProductLevel, reqVO.getProductLevel())
                .eqIfPresent(HcRouteDO::getVersionNo, reqVO.getVersionNo())
                .eqIfPresent(HcRouteDO::getRouteType, reqVO.getRouteType())
                .eqIfPresent(HcRouteDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcRouteDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcRouteDO::getId);
    }
}
