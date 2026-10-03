// 完整路径: cn.iocoder.yudao.module.mes.dal.mysql.route.RouteMapper
package cn.iocoder.yudao.module.mes.dal.mysql.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.*;

/**
 * MES工艺路线主表 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface RouteMapper extends BaseMapperX<RouteDO> {

    default PageResult<RouteDO> selectPage(RoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RouteDO>()
                .eqIfPresent(RouteDO::getCode, reqVO.getCode())
                .likeIfPresent(RouteDO::getName, reqVO.getName())
                .eqIfPresent(RouteDO::getProductId, reqVO.getProductId())
                .likeIfPresent(RouteDO::getProductName, reqVO.getProductName())
                .eqIfPresent(RouteDO::getVersion, reqVO.getVersion())
                .eqIfPresent(RouteDO::getActive, reqVO.getActive())
                .eqIfPresent(RouteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(RouteDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RouteDO::getId));
    }

}
