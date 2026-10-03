package cn.iocoder.yudao.module.mes.dal.mysql.material;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES物料主数据 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface MesMaterialMapper extends BaseMapperX<MesMaterialDO> {

    default PageResult<MesMaterialDO> selectPage(MesMaterialPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesMaterialDO>()
                .eqIfPresent(MesMaterialDO::getCode, reqVO.getCode())
                .likeIfPresent(MesMaterialDO::getName, reqVO.getName())
                .eqIfPresent(MesMaterialDO::getCategory, reqVO.getCategory())
                .eqIfPresent(MesMaterialDO::getMaterialGrade, reqVO.getMaterialGrade())
                .eqIfPresent(MesMaterialDO::getSpec, reqVO.getSpec())
                .eqIfPresent(MesMaterialDO::getUnit, reqVO.getUnit())
                .eqIfPresent(MesMaterialDO::getUnitWeight, reqVO.getUnitWeight())
                .eqIfPresent(MesMaterialDO::getScrapRate, reqVO.getScrapRate())
                .eqIfPresent(MesMaterialDO::getRemark, reqVO.getRemark())
                .eqIfPresent(MesMaterialDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesMaterialDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesMaterialDO::getId));
    }

}
