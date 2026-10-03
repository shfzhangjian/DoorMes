package cn.iocoder.yudao.module.mes.service.hc.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.team.HcTeamDO;

import java.util.List;

public interface HcTeamService {
    Long createHcTeam(HcTeamSaveReqVO createReqVO);
    void updateHcTeam(HcTeamSaveReqVO updateReqVO);
    void deleteHcTeam(Long id);
    void deleteHcTeamListByIds(List<Long> ids);
    HcTeamDO getHcTeam(Long id);
    List<HcTeamDO> getHcTeamSimpleList();
    List<HcTeamDO> getHcTeamList(HcTeamPageReqVO reqVO);
    PageResult<HcTeamDO> getHcTeamPage(HcTeamPageReqVO pageReqVO);
}