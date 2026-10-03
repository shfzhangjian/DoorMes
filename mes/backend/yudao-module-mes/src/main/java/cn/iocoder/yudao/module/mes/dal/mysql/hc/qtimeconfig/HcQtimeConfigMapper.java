package cn.iocoder.yudao.module.mes.dal.mysql.hc.qtimeconfig;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig.HcQtimeConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcQtimeConfigMapper extends BaseMapperX<HcQtimeConfigDO> {

    default PageResult<HcQtimeConfigDO> selectPage(HcQtimeConfigPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcQtimeConfigDO> selectList(HcQtimeConfigPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default List<HcQtimeConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<HcQtimeConfigDO>()
                .eq(HcQtimeConfigDO::getStatus, "ENABLED")
                .orderByAsc(HcQtimeConfigDO::getModelPrefix)
                .orderByDesc(HcQtimeConfigDO::getId));
    }

    default HcQtimeConfigDO selectByModelPrefix(String modelPrefix) {
        return selectOne(new LambdaQueryWrapperX<HcQtimeConfigDO>()
                .eq(HcQtimeConfigDO::getModelPrefix, modelPrefix)
                .last("LIMIT 1"));
    }

    default LambdaQueryWrapperX<HcQtimeConfigDO> buildQuery(HcQtimeConfigPageReqVO reqVO) {
        LambdaQueryWrapperX<HcQtimeConfigDO> query = new LambdaQueryWrapperX<HcQtimeConfigDO>()
                .likeIfPresent(HcQtimeConfigDO::getModelPrefix, reqVO.getModelPrefix())
                .eqIfPresent(HcQtimeConfigDO::getStatus, reqVO.getStatus());
        query.orderByAsc(HcQtimeConfigDO::getModelPrefix);
        query.orderByDesc(HcQtimeConfigDO::getId);
        return query;
    }

}
