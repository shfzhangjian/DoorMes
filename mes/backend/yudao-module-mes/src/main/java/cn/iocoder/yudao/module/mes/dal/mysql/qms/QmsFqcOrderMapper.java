package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcOrderMapper extends BaseMapperX<QmsFqcOrderDO> {

    String SOURCE_MODULE_CUT_ROUND_FQC = "CUT_ROUND_FQC";
    String SOURCE_MODULE_FG_SHIPPING_FQC = "FG_SHIPPING_FQC";

    default PageResult<QmsFqcOrderDO> selectPage(QmsFqcPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsFqcOrderDO> wrapper = new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .likeIfPresent(QmsFqcOrderDO::getFqcNo, reqVO.getFqcNo())
                .likeIfPresent(QmsFqcOrderDO::getReportNo, reqVO.getReportNo())
                .likeIfPresent(QmsFqcOrderDO::getWorkOrderNo, reqVO.getWorkOrderNo())
                .likeIfPresent(QmsFqcOrderDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsFqcOrderDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(QmsFqcOrderDO::getInspectionCategory, reqVO.getInspectionCategory())
                .eqIfPresent(QmsFqcOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsFqcOrderDO::getJudgment, reqVO.getJudgment())
                .eqIfPresent(QmsFqcOrderDO::getRecheckFlag, reqVO.getRecheckFlag())
                .betweenIfPresent(QmsFqcOrderDO::getInspectionTime, reqVO.getInspectionTime())
                .orderByDesc(QmsFqcOrderDO::getId);
        wrapper.and(item -> item.notIn(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_CUT_ROUND_FQC, SOURCE_MODULE_FG_SHIPPING_FQC)
                .or()
                .isNull(QmsFqcOrderDO::getSourceModule));
        return selectPage(reqVO, wrapper);
    }

    default QmsFqcOrderDO selectByFqcNo(String FqcNo, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getFqcNo, FqcNo)
                .neIfPresent(QmsFqcOrderDO::getId, excludeId));
    }

    default QmsFqcOrderDO selectLatestBySource(String sourceModule, Long sourceReportId) {
        if (sourceReportId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eqIfPresent(QmsFqcOrderDO::getSourceModule, sourceModule)
                .eq(QmsFqcOrderDO::getSourceReportId, sourceReportId)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .orderByDesc(QmsFqcOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsFqcOrderDO> selectListBySource(String sourceModule, Long sourceReportId) {
        if (sourceReportId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eqIfPresent(QmsFqcOrderDO::getSourceModule, sourceModule)
                .eq(QmsFqcOrderDO::getSourceReportId, sourceReportId)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .orderByAsc(QmsFqcOrderDO::getId));
    }

    default List<QmsFqcOrderDO> selectListBySourceReportIds(String sourceModule,
                                                            Collection<Long> sourceReportIds) {
        if (sourceReportIds == null || sourceReportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eqIfPresent(QmsFqcOrderDO::getSourceModule, sourceModule)
                .in(QmsFqcOrderDO::getSourceReportId, sourceReportIds)
                .eq(QmsFqcOrderDO::getDeleted, false)
                .orderByDesc(QmsFqcOrderDO::getSubmissionTime)
                .orderByDesc(QmsFqcOrderDO::getId));
    }

    default List<QmsFqcOrderDO> selectListByStatuses(Collection<String> statuses) {
        LambdaQueryWrapperX<QmsFqcOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(QmsFqcOrderDO::getStatus, statuses);
        wrapper.orderByAsc(QmsFqcOrderDO::getCreateTime);
        wrapper.and(item -> item.notIn(QmsFqcOrderDO::getSourceModule, SOURCE_MODULE_CUT_ROUND_FQC, SOURCE_MODULE_FG_SHIPPING_FQC)
                .or()
                .isNull(QmsFqcOrderDO::getSourceModule));
        return selectList(wrapper);
    }

    default void updateAuditNotifyTime(Long id, LocalDateTime auditNotifyTime) {
        update(null, new LambdaUpdateWrapper<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getId, id)
                .set(QmsFqcOrderDO::getAuditNotifyTime, auditNotifyTime));
    }

    default void clearAuditNotifyTime(Long id) {
        updateAuditNotifyTime(id, null);
    }
}
