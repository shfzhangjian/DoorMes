package cn.iocoder.yudao.module.mes.dal.mysql.hc.terminal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.terminal.HcTerminalDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcTerminalMapper extends BaseMapperX<HcTerminalDO> {

    default PageResult<HcTerminalDO> selectPage(HcTerminalPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcTerminalDO> selectList(HcTerminalPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcTerminalDO> buildQuery(HcTerminalPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcTerminalDO>()
                .likeIfPresent(HcTerminalDO::getTerminalCode, reqVO.getTerminalCode())
                .likeIfPresent(HcTerminalDO::getTerminalName, reqVO.getTerminalName())
                .eqIfPresent(HcTerminalDO::getWorkCenterId, reqVO.getWorkCenterId())
                .likeIfPresent(HcTerminalDO::getWorkCenterCode, reqVO.getWorkCenterCode())
                .eqIfPresent(HcTerminalDO::getTerminalMode, reqVO.getTerminalMode())
                .eqIfPresent(HcTerminalDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcTerminalDO::getId);
    }
}