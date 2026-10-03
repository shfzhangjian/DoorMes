package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRecheckHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRespVO;
import java.util.Collection;

public interface QmsProductAbnormalEventService {

    PageResult<QmsProductAbnormalEventRespVO> getPage(QmsProductAbnormalEventPageReqVO reqVO);

    QmsProductAbnormalEventDetailRespVO getDetail(String sourceType, Long inspectionId);

    QmsProductAbnormalEventRejectRespVO rejectRecheck(QmsProductAbnormalEventRejectReqVO reqVO);

    QmsProductAbnormalEventRecheckHistoryRespVO getRecheckHistory(String sourceType, Long inspectionId);

    QmsProductAbnormalEventTaskRecheckResult createTaskRecheck(String sourceType, Long inspectionId,
                                                               Collection<Long> selectedItemIds,
                                                               String rejectReason, Long dispatchTaskId);

    void bindDispatchTask(Long groupId, Long dispatchTaskId, Long rootDetailId, Long rootRoundId,
                          Long sourceDetailId, Long sourceRoundId, Long newDetailId, Long newRoundId);
}
