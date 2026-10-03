package cn.iocoder.yudao.module.mes.dal.mysql.hc.owner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.owner.HcOwnerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcOwnerMapper extends BaseMapperX<HcOwnerDO> {

    default PageResult<HcOwnerDO> selectPage(HcOwnerPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcOwnerDO> selectList(HcOwnerPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcOwnerDO> buildQuery(HcOwnerPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcOwnerDO>()
                .likeIfPresent(HcOwnerDO::getOwnerCode, reqVO.getOwnerCode())
                .likeIfPresent(HcOwnerDO::getOwnerName, reqVO.getOwnerName())
                .eqIfPresent(HcOwnerDO::getOwnerType, reqVO.getOwnerType())
                .likeIfPresent(HcOwnerDO::getContactName, reqVO.getContactName())
                .eqIfPresent(HcOwnerDO::getContactPhone, reqVO.getContactPhone())
                .eqIfPresent(HcOwnerDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcOwnerDO::getId);
    }
}