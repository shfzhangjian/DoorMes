package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolLedgerDO;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolLedgerMapper extends BaseMapperX<QmsMeasureToolLedgerDO> {

    default PageResult<QmsMeasureToolLedgerDO> selectPage(QmsMeasureToolLedgerPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsMeasureToolLedgerDO> query = new LambdaQueryWrapperX<QmsMeasureToolLedgerDO>()
                .eqIfPresent(QmsMeasureToolLedgerDO::getToolCode, reqVO.getToolCode())
                .eqIfPresent(QmsMeasureToolLedgerDO::getBodyNo, reqVO.getBodyNo())
                .likeIfPresent(QmsMeasureToolLedgerDO::getToolName, reqVO.getToolName())
                .eqIfPresent(QmsMeasureToolLedgerDO::getStorageLocation, reqVO.getStorageLocation())
                .likeIfPresent(QmsMeasureToolLedgerDO::getUsingDepartment, reqVO.getUsingDepartment())
                .likeIfPresent(QmsMeasureToolLedgerDO::getKeeperName, reqVO.getKeeperName())
                .eqIfPresent(QmsMeasureToolLedgerDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsMeasureToolLedgerDO::getCalibrationStatus, reqVO.getCalibrationStatus())
                .eqIfPresent(QmsMeasureToolLedgerDO::getMsaStatus, reqVO.getMsaStatus())
                .eqIfPresent(QmsMeasureToolLedgerDO::getMsaEnabled, reqVO.getMsaEnabled())
                .eqIfPresent(QmsMeasureToolLedgerDO::getExternalOpen, reqVO.getExternalOpen())
                .likeIfPresent(QmsMeasureToolLedgerDO::getResponsiblePerson, reqVO.getResponsiblePerson())
                .betweenIfPresent(QmsMeasureToolLedgerDO::getNextCalibrationDate, reqVO.getNextCalibrationDate())
                .betweenIfPresent(QmsMeasureToolLedgerDO::getCreateTime, reqVO.getCreateTime());
        if (reqVO.getStatusValues() != null && reqVO.getStatusValues().length > 0) {
            query.in(QmsMeasureToolLedgerDO::getStatus, Arrays.asList(reqVO.getStatusValues()));
        }
        if (reqVO.getCalibrationStatusValues() != null && reqVO.getCalibrationStatusValues().length > 0) {
            query.in(QmsMeasureToolLedgerDO::getCalibrationStatus, Arrays.asList(reqVO.getCalibrationStatusValues()));
        }
        if (reqVO.getMsaStatusValues() != null && reqVO.getMsaStatusValues().length > 0) {
            query.in(QmsMeasureToolLedgerDO::getMsaStatus, Arrays.asList(reqVO.getMsaStatusValues()));
        }
        if (reqVO.getMsaEnabledValues() != null && reqVO.getMsaEnabledValues().length > 0) {
            query.in(QmsMeasureToolLedgerDO::getMsaEnabled, Arrays.asList(reqVO.getMsaEnabledValues()));
        }
        if (reqVO.getCategoryIds() != null && reqVO.getCategoryIds().length > 0) {
            query.in(QmsMeasureToolLedgerDO::getCategoryId, Arrays.asList(reqVO.getCategoryIds()));
        } else {
            query.eqIfPresent(QmsMeasureToolLedgerDO::getCategoryId, reqVO.getCategoryId());
        }
        applyReminderScope(query, reqVO);
        if (Boolean.TRUE.equals(reqVO.getCalibrationOverdue())) {
            query.isNotNull(QmsMeasureToolLedgerDO::getNextCalibrationDate)
                    .lt(QmsMeasureToolLedgerDO::getNextCalibrationDate, LocalDate.now());
        }
        applySort(query, reqVO);
        return selectPage(reqVO, query);
    }

    private static void applyReminderScope(LambdaQueryWrapperX<QmsMeasureToolLedgerDO> query,
                                           QmsMeasureToolLedgerPageReqVO reqVO) {
        LocalDate today = LocalDate.now();
        boolean onlyOverdue = Boolean.TRUE.equals(reqVO.getOnlyOverdue());
        boolean onlyCurrentMonthReminder = Boolean.TRUE.equals(reqVO.getOnlyCurrentMonthReminder());
        if (!onlyOverdue && !onlyCurrentMonthReminder) {
            return;
        }
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        if (onlyOverdue && onlyCurrentMonthReminder) {
            query.and(wrapper -> wrapper
                    .nested(overdueWrapper -> overdueWrapper.lt(QmsMeasureToolLedgerDO::getNextCalibrationDate, today)
                            .or(msaWrapper -> msaWrapper.eq(QmsMeasureToolLedgerDO::getMsaEnabled, 1)
                                    .lt(QmsMeasureToolLedgerDO::getNextMsaDate, today)))
                    .or(currentMonthWrapper -> currentMonthWrapper
                            .between(QmsMeasureToolLedgerDO::getNextCalibrationDate, monthStart, monthEnd)
                            .or(msaWrapper -> msaWrapper.eq(QmsMeasureToolLedgerDO::getMsaEnabled, 1)
                                    .between(QmsMeasureToolLedgerDO::getNextMsaDate, monthStart, monthEnd))));
            return;
        }
        if (onlyOverdue) {
            query.and(wrapper -> wrapper.lt(QmsMeasureToolLedgerDO::getNextCalibrationDate, today)
                    .or(msaWrapper -> msaWrapper.eq(QmsMeasureToolLedgerDO::getMsaEnabled, 1)
                            .lt(QmsMeasureToolLedgerDO::getNextMsaDate, today)));
        }
        if (onlyCurrentMonthReminder) {
            query.and(wrapper -> wrapper.between(QmsMeasureToolLedgerDO::getNextCalibrationDate, monthStart, monthEnd)
                    .or(msaWrapper -> msaWrapper.eq(QmsMeasureToolLedgerDO::getMsaEnabled, 1)
                            .between(QmsMeasureToolLedgerDO::getNextMsaDate, monthStart, monthEnd)));
        }
    }

    private static void applySort(LambdaQueryWrapperX<QmsMeasureToolLedgerDO> query,
                                  QmsMeasureToolLedgerPageReqVO reqVO) {
        boolean ascending = !"desc".equalsIgnoreCase(reqVO.getSortOrder());
        switch (reqVO.getSortField() == null ? "" : reqVO.getSortField()) {
            case "bodyNo" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getBodyNo);
            case "toolCode" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getToolCode);
            case "toolName" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getToolName);
            case "model" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getModel);
            case "categoryName" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getCategoryName);
            case "manufacturer" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getManufacturer);
            case "purchaseDate" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getPurchaseDate);
            case "calibrationCycleMonths" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getCalibrationCycleMonths);
            case "lastCalibrationDate" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getLastCalibrationDate);
            case "nextCalibrationDate" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getNextCalibrationDate);
            case "maintainerName" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getMaintainerName);
            case "storageLocation" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getStorageLocation);
            case "status" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getStatus);
            case "calibrationResult" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getCalibrationResult);
            case "lastMsaDate" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getLastMsaDate);
            case "nextMsaDate" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getNextMsaDate);
            case "responsiblePerson" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getResponsiblePerson);
            case "externalOpen" -> query.orderBy(true, ascending, QmsMeasureToolLedgerDO::getExternalOpen);
            default -> query.orderByAsc(QmsMeasureToolLedgerDO::getNextCalibrationDate)
                    .orderByDesc(QmsMeasureToolLedgerDO::getId);
        }
    }

    default List<QmsMeasureToolLedgerDO> selectWarningCandidates(LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<QmsMeasureToolLedgerDO>()
                .notIn(QmsMeasureToolLedgerDO::getStatus, List.of("STOPPED", "SCRAPPED"))
                .isNotNull(QmsMeasureToolLedgerDO::getNextCalibrationDate)
                .le(QmsMeasureToolLedgerDO::getNextCalibrationDate, endDate)
                .orderByAsc(QmsMeasureToolLedgerDO::getNextCalibrationDate));
    }

    default List<QmsMeasureToolLedgerDO> selectCalibrationTaskCandidates(Long categoryId) {
        return selectList(new LambdaQueryWrapperX<QmsMeasureToolLedgerDO>()
                .eqIfPresent(QmsMeasureToolLedgerDO::getCategoryId, categoryId)
                .notIn(QmsMeasureToolLedgerDO::getStatus, List.of("STOPPED", "SCRAPPED"))
                .isNotNull(QmsMeasureToolLedgerDO::getNextCalibrationDate)
                .orderByAsc(QmsMeasureToolLedgerDO::getNextCalibrationDate)
                .orderByAsc(QmsMeasureToolLedgerDO::getToolCode)
                .orderByDesc(QmsMeasureToolLedgerDO::getId));
    }

    default QmsMeasureToolLedgerDO selectByToolCode(String toolCode) {
        return selectOne(QmsMeasureToolLedgerDO::getToolCode, toolCode);
    }

    default long selectCountByCategoryId(Long categoryId) {
        return selectCount(QmsMeasureToolLedgerDO::getCategoryId, categoryId);
    }

}
