package cn.iocoder.yudao.module.mes.dal.mysql.workshop;

import java.util.*;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workshop.MesWorkshopDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES车间产线定义 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface MesWorkshopMapper extends BaseMapperX<MesWorkshopDO> {

    default List<MesWorkshopDO> selectList(MesWorkshopListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MesWorkshopDO>()
                .eqIfPresent(MesWorkshopDO::getParentId, reqVO.getParentId())
                .eqIfPresent(MesWorkshopDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWorkshopDO::getName, reqVO.getName())
                .eqIfPresent(MesWorkshopDO::getType, reqVO.getType())
                .eqIfPresent(MesWorkshopDO::getManager, reqVO.getManager())
                .eqIfPresent(MesWorkshopDO::getArea, reqVO.getArea())
                .eqIfPresent(MesWorkshopDO::getSort, reqVO.getSort())
                .eqIfPresent(MesWorkshopDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesWorkshopDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesWorkshopDO::getId));
    }

	default MesWorkshopDO selectByParentIdAndId(Long parentId, String id) {
	    return selectOne(MesWorkshopDO::getParentId, parentId, MesWorkshopDO::getId, id);
	}

    default Long selectCountByParentId(Long parentId) {
        return selectCount(MesWorkshopDO::getParentId, parentId);
    }

}
