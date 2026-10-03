package cn.iocoder.yudao.module.mes.service.qms;

import java.util.Map;

/**
 * 产品异常事件接入质量任务中心后的复检复制结果。
 */
public record QmsProductAbnormalEventTaskRecheckResult(
        String sourceType,
        Long groupId,
        Long rootInspectionId,
        String rootInspectionNo,
        Long rootDetailId,
        Long sourceInspectionId,
        String sourceInspectionNo,
        Long sourceDetailId,
        Long newInspectionId,
        String newInspectionNo,
        Long newDetailId,
        Integer roundNo,
        Integer taskRoundNo,
        Long tenantId,
        Map<Long, Long> itemIdMap) {
}
