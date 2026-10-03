package cn.iocoder.yudao.module.mes.dal.mysql.bom;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.bom.BomDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.mes.controller.admin.bom.vo.*;

/**
 * 工艺BOM主表 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface BomMapper extends BaseMapperX<BomDO> {

    default PageResult<BomDO> selectPage(BomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BomDO>()
                .eqIfPresent(BomDO::getProductId, reqVO.getProductId())
                .likeIfPresent(BomDO::getProductName, reqVO.getProductName())
                .eqIfPresent(BomDO::getVersion, reqVO.getVersion())
                .eqIfPresent(BomDO::getActive, reqVO.getActive())
                .eqIfPresent(BomDO::getRemark, reqVO.getRemark())
                .eqIfPresent(BomDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BomDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BomDO::getId));
    }

}
