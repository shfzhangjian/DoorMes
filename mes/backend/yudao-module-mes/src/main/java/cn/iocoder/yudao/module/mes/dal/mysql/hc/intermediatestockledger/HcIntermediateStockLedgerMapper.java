package cn.iocoder.yudao.module.mes.dal.mysql.hc.intermediatestockledger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.SortingField;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.intermediatestockledger.HcIntermediateStockLedgerDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface HcIntermediateStockLedgerMapper extends BaseMapperX<HcIntermediateStockLedgerDO> {

    String STOCK_TYPE_WIP = "WIP";

    default PageResult<HcIntermediateStockLedgerDO> selectPage(HcIntermediateStockLedgerPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default LambdaQueryWrapper<HcIntermediateStockLedgerDO> buildQuery(HcIntermediateStockLedgerPageReqVO reqVO) {
        LocalDateTime txnTimeStart = reqVO.getTxnDateStart() == null
                ? null
                : reqVO.getTxnDateStart().atStartOfDay();
        LocalDateTime txnTimeEndExclusive = reqVO.getTxnDateEnd() == null
                ? null
                : reqVO.getTxnDateEnd().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<HcIntermediateStockLedgerDO> query = new LambdaQueryWrapperX<HcIntermediateStockLedgerDO>()
                .eq(HcIntermediateStockLedgerDO::getStockType, STOCK_TYPE_WIP)
                .likeIfPresent(HcIntermediateStockLedgerDO::getSourcePlanNo, reqVO.getSourcePlanNo())
                .eqIfPresent(HcIntermediateStockLedgerDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(HcIntermediateStockLedgerDO::getBatchNo, reqVO.getBatchNo())
                .likeIfPresent(HcIntermediateStockLedgerDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .likeIfPresent(HcIntermediateStockLedgerDO::getSourceParentBatchNo, reqVO.getSourceParentBatchNo())
                .likeIfPresent(HcIntermediateStockLedgerDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcIntermediateStockLedgerDO::getModelNo, reqVO.getModelNo())
                .eqIfPresent(HcIntermediateStockLedgerDO::getQualityStatus, reqVO.getQualityStatus())
                .geIfPresent(HcIntermediateStockLedgerDO::getLastTxnTime, txnTimeStart)
                .ltIfPresent(HcIntermediateStockLedgerDO::getLastTxnTime, txnTimeEndExclusive);
        if (Boolean.TRUE.equals(reqVO.getOnlyAvailable())) {
            query.gt(HcIntermediateStockLedgerDO::getAvailableQty, BigDecimal.ZERO);
        }
        applyStockStatusFilter(query, reqVO.getStockStatus());
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper
                    .like(HcIntermediateStockLedgerDO::getSourcePlanNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getSourceBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getSourceParentBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getMaterialCode, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getMaterialName, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getModelNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getOpName, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getLastTxnNo, reqVO.getKeyword())
                    .or()
                    .like(HcIntermediateStockLedgerDO::getBusinessRemark, reqVO.getKeyword()));
        }
        applySorting(query, reqVO);
        return query;
    }

    private static void applyStockStatusFilter(LambdaQueryWrapper<HcIntermediateStockLedgerDO> query, String stockStatus) {
        if (stockStatus == null || stockStatus.isBlank()) {
            return;
        }
        switch (stockStatus) {
            case "AVAILABLE" -> query.gt(HcIntermediateStockLedgerDO::getAvailableQty, BigDecimal.ZERO);
            case "LOCKED" -> query.gt(HcIntermediateStockLedgerDO::getPlanLockedQty, BigDecimal.ZERO);
            case "FROZEN" -> query.gt(HcIntermediateStockLedgerDO::getFrozenQty, BigDecimal.ZERO);
            case "CONSUMED" -> query.le(HcIntermediateStockLedgerDO::getOnHandQty, BigDecimal.ZERO);
            default -> {
            }
        }
    }

    private static void applySorting(LambdaQueryWrapper<HcIntermediateStockLedgerDO> query,
                                     HcIntermediateStockLedgerPageReqVO reqVO) {
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

    private static boolean applySortingField(LambdaQueryWrapper<HcIntermediateStockLedgerDO> query,
                                             String field, boolean asc) {
        if ("sourceType".equals(field)) {
            query.orderBy(true, asc, HcIntermediateStockLedgerDO::getOpSeq)
                    .orderBy(true, asc, HcIntermediateStockLedgerDO::getOpName);
            return true;
        }
        SFunction<HcIntermediateStockLedgerDO, ?> column = switch (field) {
            case "sourcePlanNo" -> HcIntermediateStockLedgerDO::getSourcePlanNo;
            case "sourceParentBatchNo", "motherBatchNo" -> HcIntermediateStockLedgerDO::getSourceParentBatchNo;
            case "batchNo" -> HcIntermediateStockLedgerDO::getBatchNo;
            case "modelNo" -> HcIntermediateStockLedgerDO::getModelNo;
            case "onHandQty" -> HcIntermediateStockLedgerDO::getOnHandQty;
            case "availableQty" -> HcIntermediateStockLedgerDO::getAvailableQty;
            case "shareableQty" -> HcIntermediateStockLedgerDO::getShareableQty;
            case "planLockedQty" -> HcIntermediateStockLedgerDO::getPlanLockedQty;
            case "lastTxnTime" -> HcIntermediateStockLedgerDO::getLastTxnTime;
            case "businessRemark" -> HcIntermediateStockLedgerDO::getBusinessRemark;
            default -> null;
        };
        if (column == null) {
            return false;
        }
        query.orderBy(true, asc, column);
        return true;
    }

    private static void applyDefaultSorting(LambdaQueryWrapper<HcIntermediateStockLedgerDO> query) {
        query.orderByDesc(HcIntermediateStockLedgerDO::getLastTxnTime)
                .orderByAsc(HcIntermediateStockLedgerDO::getOpSeq)
                .orderByAsc(HcIntermediateStockLedgerDO::getBatchNo);
    }

    private static void applyStableSorting(LambdaQueryWrapper<HcIntermediateStockLedgerDO> query, List<String> sortedFields) {
        if (!sortedFields.contains("lastTxnTime")) {
            query.orderByDesc(HcIntermediateStockLedgerDO::getLastTxnTime);
        }
        if (!sortedFields.contains("sourceType")) {
            query.orderByAsc(HcIntermediateStockLedgerDO::getOpSeq);
        }
        if (!sortedFields.contains("batchNo")) {
            query.orderByAsc(HcIntermediateStockLedgerDO::getBatchNo);
        }
    }

}
