package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeRespVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QmsQualityStandardRoleScopeService {

    record CurrentUserStandardScope(boolean unrestricted, Set<Long> standardIds) {
    }

    CurrentUserStandardScope getCurrentUserStandardScope();

    PageResult<QmsQualityStandardRoleScopeRespVO> getScopePage(@Valid QmsQualityStandardRoleScopePageReqVO pageReqVO);

    PageResult<QmsQualityStandardRoleScopeRespVO> getCandidatePage(@Valid QmsQualityStandardRoleScopePageReqVO pageReqVO);

    Map<String, Long> getScopeCount(Long roleId);

    void addScopes(@Valid QmsQualityStandardRoleScopeAddReqVO reqVO);

    void removeScopes(List<Long> ids);
}
