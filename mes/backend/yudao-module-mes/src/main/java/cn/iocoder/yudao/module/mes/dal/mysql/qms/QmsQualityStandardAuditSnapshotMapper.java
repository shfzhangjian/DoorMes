package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardAuditSnapshotDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsQualityStandardAuditSnapshotMapper extends BaseMapperX<QmsQualityStandardAuditSnapshotDO> {

    default QmsQualityStandardAuditSnapshotDO selectLatestPendingByStandardId(Long standardId) {
        return selectOne(new LambdaQueryWrapper<QmsQualityStandardAuditSnapshotDO>()
                .eq(QmsQualityStandardAuditSnapshotDO::getStandardId, standardId)
                .eq(QmsQualityStandardAuditSnapshotDO::getSnapshotStatus, "PENDING")
                .orderByDesc(QmsQualityStandardAuditSnapshotDO::getId)
                .last("LIMIT 1"));
    }

    default void markAuditResult(Long id, String auditResult, String rejectReason, Long auditUserId,
                                 String auditUserName, LocalDateTime auditTime, String snapshotStatus) {
        update(null, new LambdaUpdateWrapper<QmsQualityStandardAuditSnapshotDO>()
                .eq(QmsQualityStandardAuditSnapshotDO::getId, id)
                .set(QmsQualityStandardAuditSnapshotDO::getAuditResult, auditResult)
                .set(QmsQualityStandardAuditSnapshotDO::getRejectReason, rejectReason)
                .set(QmsQualityStandardAuditSnapshotDO::getAuditUserId, auditUserId)
                .set(QmsQualityStandardAuditSnapshotDO::getAuditUserName, auditUserName)
                .set(QmsQualityStandardAuditSnapshotDO::getAuditTime, auditTime)
                .set(QmsQualityStandardAuditSnapshotDO::getSnapshotStatus, snapshotStatus));
    }
}
