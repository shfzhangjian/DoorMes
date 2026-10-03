package cn.iocoder.yudao.module.mes.service.hc.researchtask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.researchtask.HcResearchTaskDO;
import java.util.List;

public interface HcResearchTaskService {

    Long createResearchTask(HcResearchTaskSaveReqVO createReqVO);

    void updateResearchTask(HcResearchTaskSaveReqVO updateReqVO);

    void deleteResearchTask(Long id);

    void deleteResearchTaskListByIds(List<Long> ids);

    HcResearchTaskDO getResearchTask(Long id);

    PageResult<HcResearchTaskDO> getResearchTaskPage(HcResearchTaskPageReqVO pageReqVO);

    List<HcResearchTaskDO> getResearchTaskList(HcResearchTaskPageReqVO reqVO);

    HcResearchTaskCodePreviewRespVO previewModelCode(HcResearchTaskCodePreviewReqVO reqVO);

    void confirmResearchTask(Long id);

    Long archiveToProductModel(Long id);

}
