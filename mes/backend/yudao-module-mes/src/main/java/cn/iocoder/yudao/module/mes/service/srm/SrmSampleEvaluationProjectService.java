package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectSaveReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmSampleEvaluationProjectService {

    Long createProject(@Valid SrmSampleEvaluationProjectSaveReqVO reqVO);

    void updateProject(@Valid SrmSampleEvaluationProjectSaveReqVO reqVO);

    void deleteProject(Long id);

    SrmSampleEvaluationProjectRespVO getProject(Long id);

    PageResult<SrmSampleEvaluationProjectRespVO> getProjectPage(SrmSampleEvaluationProjectPageReqVO reqVO);

    List<SrmSampleEvaluationProjectRespVO> getEnabledProjectList();

}
