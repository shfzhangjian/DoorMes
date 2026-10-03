package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectScorerConfigReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmPreliminaryProjectService {

    Long createProject(@Valid SrmPreliminaryProjectSaveReqVO reqVO);

    void updateProject(@Valid SrmPreliminaryProjectSaveReqVO reqVO);

    void deleteProject(Long id);

    SrmPreliminaryProjectRespVO getProject(Long id);

    PageResult<SrmPreliminaryProjectRespVO> getProjectPage(SrmPreliminaryProjectPageReqVO reqVO);

    List<SrmPreliminaryProjectRespVO> getEnabledProjectList();

    SrmPreliminaryProjectRespVO getScorerConfig(Long projectId, Long templateVersionId);

    void saveScorerConfig(@Valid SrmPreliminaryProjectScorerConfigReqVO reqVO);

}
