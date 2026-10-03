package cn.iocoder.yudao.module.mes.service.hc.packagingevent;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import java.time.LocalDateTime;

/** 待包装片不合格生命周期事件写入。 */
public interface HcPackagingPieceEventLogService {

    void recordCutRoundFqcResult(HcCutRoundReportDO report, String fqcResult, String operatorName,
                                 LocalDateTime eventTime, String refDocNo, String remark);

    void recordCutRoundEvent(HcCutRoundReportDO report, String eventType, String beforeStatus,
                             String afterStatus, LocalDateTime eventTime, String operatorName,
                             String refDocNo, String remark);

    void recordManualPieceEvent(HcPackagingManualPieceDO piece, String eventType, String beforeStatus,
                                String afterStatus, LocalDateTime eventTime, String operatorName,
                                String refDocNo, String remark);
}
