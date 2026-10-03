package cn.iocoder.yudao.module.mes.dal.mysql.hc.processoutputbalance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.SortingField;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo.HcProcessOutputBalancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processoutputbalance.HcProcessOutputBalanceDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface HcProcessOutputBalanceMapper extends BaseMapperX<HcProcessOutputBalanceDO> {

    default PageResult<HcProcessOutputBalanceDO> selectPage(HcProcessOutputBalancePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default LambdaQueryWrapper<HcProcessOutputBalanceDO> buildQuery(HcProcessOutputBalancePageReqVO reqVO) {
        LocalDateTime reportTimeStart = reqVO.getReportDateStart() == null
                ? null
                : reqVO.getReportDateStart().atStartOfDay();
        LocalDateTime reportTimeEndExclusive = reqVO.getReportDateEnd() == null
                ? null
                : reqVO.getReportDateEnd().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<HcProcessOutputBalanceDO> query = new LambdaQueryWrapperX<HcProcessOutputBalanceDO>()
                .likeIfPresent(HcProcessOutputBalanceDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(HcProcessOutputBalanceDO::getStageCode, reqVO.getStageCode())
                .eqIfPresent(HcProcessOutputBalanceDO::getBalanceStatus, reqVO.getBalanceStatus())
                .likeIfPresent(HcProcessOutputBalanceDO::getOutputBatchNo, reqVO.getOutputBatchNo())
                .likeIfPresent(HcProcessOutputBalanceDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .likeIfPresent(HcProcessOutputBalanceDO::getParentBatchNo, reqVO.getParentBatchNo())
                .likeIfPresent(HcProcessOutputBalanceDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcProcessOutputBalanceDO::getModelCode, reqVO.getModelCode())
                .geIfPresent(HcProcessOutputBalanceDO::getReportTime, reportTimeStart)
                .ltIfPresent(HcProcessOutputBalanceDO::getReportTime, reportTimeEndExclusive);
        if (Boolean.TRUE.equals(reqVO.getOnlyRemaining())) {
            query.gt(HcProcessOutputBalanceDO::getRemainingQty, BigDecimal.ZERO);
        }
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper
                    .like(HcProcessOutputBalanceDO::getPlanNo, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getOutputBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getSourceBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getParentBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getMaterialCode, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getMaterialName, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getModelCode, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getStageName, reqVO.getKeyword())
                    .or()
                    .like(HcProcessOutputBalanceDO::getConsumeSummary, reqVO.getKeyword()));
        }
        applySorting(query, reqVO);
        return query;
    }

    private static void applySorting(LambdaQueryWrapper<HcProcessOutputBalanceDO> query,
                                     HcProcessOutputBalancePageReqVO reqVO) {
        List<SortingField> sortingFields = reqVO.getSortingFields();
        if (sortingFields == null || sortingFields.isEmpty()) {
            applyDefaultSorting(query);
            return;
        }

        List<String> sortedFields = new ArrayList<>();
        for (SortingField sortingField : sortingFields) {
            if (sortingField == null || sortingField.getField() == null || sortingField.getOrder() == null) {
                continue;
            }
            if (sortedFields.contains(sortingField.getField())) {
                continue;
            }
            boolean asc = SortingField.ORDER_ASC.equalsIgnoreCase(sortingField.getOrder());
            boolean desc = SortingField.ORDER_DESC.equalsIgnoreCase(sortingField.getOrder());
            if (!asc && !desc) {
                continue;
            }
            if (applySortingField(query, sortingField.getField(), asc)) {
                sortedFields.add(sortingField.getField());
            }
        }

        if (sortedFields.isEmpty()) {
            applyDefaultSorting(query);
            return;
        }
        applyStableSorting(query, sortedFields);
    }

    private static boolean applySortingField(LambdaQueryWrapper<HcProcessOutputBalanceDO> query,
                                             String field, boolean asc) {
        if ("stageCode".equals(field)) {
            query.orderBy(true, asc, HcProcessOutputBalanceDO::getStageSort)
                    .orderBy(true, asc, HcProcessOutputBalanceDO::getStageName);
            return true;
        }
        SFunction<HcProcessOutputBalanceDO, ?> column = switch (field) {
            case "planNo" -> HcProcessOutputBalanceDO::getPlanNo;
            case "outputBatchNo" -> HcProcessOutputBalanceDO::getOutputBatchNo;
            case "modelCode" -> HcProcessOutputBalanceDO::getModelCode;
            case "outputQty" -> HcProcessOutputBalanceDO::getOutputQty;
            case "consumedQty" -> HcProcessOutputBalanceDO::getConsumedQty;
            case "remainingQty" -> HcProcessOutputBalanceDO::getRemainingQty;
            case "balanceStatus" -> HcProcessOutputBalanceDO::getBalanceStatus;
            case "reportTime" -> HcProcessOutputBalanceDO::getReportTime;
            case "consumeSummary" -> HcProcessOutputBalanceDO::getConsumeSummary;
            default -> null;
        };
        if (column == null) {
            return false;
        }
        query.orderBy(true, asc, column);
        return true;
    }

    private static void applyDefaultSorting(LambdaQueryWrapper<HcProcessOutputBalanceDO> query) {
        query.orderByDesc(HcProcessOutputBalanceDO::getReportTime)
                .orderByAsc(HcProcessOutputBalanceDO::getStageSort)
                .orderByAsc(HcProcessOutputBalanceDO::getOutputBatchNo);
    }

    private static void applyStableSorting(LambdaQueryWrapper<HcProcessOutputBalanceDO> query, List<String> sortedFields) {
        if (!sortedFields.contains("reportTime")) {
            query.orderByDesc(HcProcessOutputBalanceDO::getReportTime);
        }
        if (!sortedFields.contains("stageCode")) {
            query.orderByAsc(HcProcessOutputBalanceDO::getStageSort);
        }
        if (!sortedFields.contains("outputBatchNo")) {
            query.orderByAsc(HcProcessOutputBalanceDO::getOutputBatchNo);
        }
    }

}
