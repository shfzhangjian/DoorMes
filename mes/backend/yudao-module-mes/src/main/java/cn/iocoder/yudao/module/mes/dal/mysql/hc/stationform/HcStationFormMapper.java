package cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcStationFormMapper extends BaseMapperX<HcStationFormDO> {

    default PageResult<HcStationFormDO> selectPage(HcStationFormPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcStationFormDO> selectList(HcStationFormPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default List<HcStationFormDO> selectEnabledByProcess(String processCode) {
        return selectList(new LambdaQueryWrapperX<HcStationFormDO>()
                .eq(HcStationFormDO::getProcessCode, processCode)
                .eq(HcStationFormDO::getStatus, 1)
                .orderByAsc(HcStationFormDO::getSortNo)
                .orderByAsc(HcStationFormDO::getId));
    }

    default List<HcStationFormDO> selectEnabledList(String processCode) {
        return selectList(new LambdaQueryWrapperX<HcStationFormDO>()
                .eqIfPresent(HcStationFormDO::getProcessCode, processCode)
                .eq(HcStationFormDO::getStatus, 1)
                .orderByAsc(HcStationFormDO::getProcessCode)
                .orderByAsc(HcStationFormDO::getSortNo)
                .orderByAsc(HcStationFormDO::getId));
    }

    default HcStationFormDO selectEnabledByCode(String formCode) {
        return selectOne(new LambdaQueryWrapperX<HcStationFormDO>()
                .eq(HcStationFormDO::getFormCode, formCode)
                .eq(HcStationFormDO::getStatus, 1));
    }

    default HcStationFormDO selectByFormCode(String formCode) {
        return selectOne(new LambdaQueryWrapperX<HcStationFormDO>()
                .eq(HcStationFormDO::getFormCode, formCode));
    }

    private LambdaQueryWrapperX<HcStationFormDO> buildQuery(HcStationFormPageReqVO reqVO) {
        LambdaQueryWrapperX<HcStationFormDO> queryWrapper = new LambdaQueryWrapperX<HcStationFormDO>()
                .likeIfPresent(HcStationFormDO::getFormCode, reqVO.getFormCode())
                .likeIfPresent(HcStationFormDO::getFormName, reqVO.getFormName())
                .eqIfPresent(HcStationFormDO::getProcessCode, reqVO.getProcessCode())
                .likeIfPresent(HcStationFormDO::getProcessName, reqVO.getProcessName())
                .eqIfPresent(HcStationFormDO::getTriggerTimingCode, reqVO.getTriggerTimingCode())
                .eqIfPresent(HcStationFormDO::getNeedConfirm, reqVO.getNeedConfirm())
                .eqIfPresent(HcStationFormDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcStationFormDO::getRemark, reqVO.getRemark());
        queryWrapper.orderByAsc(HcStationFormDO::getProcessCode)
                .orderByAsc(HcStationFormDO::getSortNo)
                .orderByDesc(HcStationFormDO::getId);
        return queryWrapper;
    }
}
