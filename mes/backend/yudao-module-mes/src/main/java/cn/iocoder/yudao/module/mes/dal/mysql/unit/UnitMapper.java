package cn.iocoder.yudao.module.mes.dal.mysql.unit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.mes.controller.admin.unit.vo.*;

/**
 * MES计量单位 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface UnitMapper extends BaseMapperX<UnitDO> {

    default PageResult<UnitDO> selectPage(UnitPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<UnitDO>()
                .eqIfPresent(UnitDO::getCode, reqVO.getCode())
                .likeIfPresent(UnitDO::getName, reqVO.getName())
                .eqIfPresent(UnitDO::getCategory, reqVO.getCategory())
                .eqIfPresent(UnitDO::getBase, reqVO.getBase())
                .eqIfPresent(UnitDO::getRatio, reqVO.getRatio())
                .eqIfPresent(UnitDO::getPrecision, reqVO.getPrecision())
                .eqIfPresent(UnitDO::getStatus, reqVO.getStatus())
                .eqIfPresent(UnitDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(UnitDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(UnitDO::getId));
    }

    default UnitDO selectOneByCodeOrName(String value) {
        return selectOne(new LambdaQueryWrapperX<UnitDO>()
                .and(wrapper -> wrapper.eq(UnitDO::getCode, value).or().eq(UnitDO::getName, value))
                .eq(UnitDO::getDeleted, false)
                .last("LIMIT 1"));
    }

}
