package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPiecePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcNgInventoryPieceMapper extends BaseMapperX<HcNgInventoryPieceDO> {

    default HcNgInventoryPieceDO selectBySource(String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getSourceType, sourceType)
                .eq(HcNgInventoryPieceDO::getSourceId, sourceId)
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcNgInventoryPieceDO selectBySourceForUpdate(String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getSourceType, sourceType)
                .eq(HcNgInventoryPieceDO::getSourceId, sourceId)
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default HcNgInventoryPieceDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getId, id)
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default boolean existsNotDeletedByPieceNo(String pieceNo) {
        if (pieceNo == null || pieceNo.isBlank()) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getPieceNo, pieceNo)
                .eq(HcNgInventoryPieceDO::getDeleted, false)) > 0;
    }

    default boolean existsNotDeletedByPieceNoExcludeId(String pieceNo, Long excludeId) {
        if (pieceNo == null || pieceNo.isBlank()) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getPieceNo, pieceNo)
                .neIfPresent(HcNgInventoryPieceDO::getId, excludeId)
                .eq(HcNgInventoryPieceDO::getDeleted, false)) > 0;
    }

    default List<HcNgInventoryPieceDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .in(HcNgInventoryPieceDO::getId, ids)
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .orderByAsc(HcNgInventoryPieceDO::getId)
                .last("FOR UPDATE"));
    }

    default List<HcNgInventoryPieceDO> selectActiveListByLocationKeys(Collection<String> locationKeys) {
        if (locationKeys == null || locationKeys.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .in(HcNgInventoryPieceDO::getCurrentLocationCode, locationKeys)
                .in(HcNgInventoryPieceDO::getStatus, List.of("STORED", "FROZEN"))
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .orderByAsc(HcNgInventoryPieceDO::getCurrentLocationCode)
                .orderByAsc(HcNgInventoryPieceDO::getId));
    }

    default List<HcNgInventoryPieceDO> selectFrozenByPlanOperationForUpdate(Long planId, Long planOperationId) {
        if (planId == null || planOperationId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getSourcePlanId, planId)
                .eq(HcNgInventoryPieceDO::getSourcePlanOperationId, planOperationId)
                .eq(HcNgInventoryPieceDO::getStatus, "FROZEN")
                .ne(HcNgInventoryPieceDO::getSourceType, "MANUAL_HISTORY")
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .orderByAsc(HcNgInventoryPieceDO::getId)
                .last("FOR UPDATE"));
    }

    default boolean existsActiveByOriginalLocationKey(String locationKey) {
        return selectCount(new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getOriginalLocationCode, locationKey)
                .eq(HcNgInventoryPieceDO::getStatus, "FROZEN")
                .eq(HcNgInventoryPieceDO::getDeleted, false)) > 0;
    }

    default PageResult<HcNgInventoryPieceDO> selectPage(NgPiecePageReqVO reqVO) {
        LambdaQueryWrapperX<HcNgInventoryPieceDO> query = new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eqIfPresent(HcNgInventoryPieceDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(HcNgInventoryPieceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcNgInventoryPieceDO::getCurrentWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(HcNgInventoryPieceDO::getCurrentLocationCode, reqVO.getLocationKey())
                .likeIfPresent(HcNgInventoryPieceDO::getModelNo, reqVO.getModelNo())
                .likeIfPresent(HcNgInventoryPieceDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(HcNgInventoryPieceDO::getPadType, reqVO.getPadType())
                .geIfPresent(HcNgInventoryPieceDO::getShelvedTime,
                        reqVO.getShelvedDateStart() == null ? null : reqVO.getShelvedDateStart().atStartOfDay())
                .ltIfPresent(HcNgInventoryPieceDO::getShelvedTime,
                        reqVO.getShelvedDateEnd() == null ? null : reqVO.getShelvedDateEnd().plusDays(1).atStartOfDay())
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .orderByDesc(HcNgInventoryPieceDO::getCreateTime)
                .orderByDesc(HcNgInventoryPieceDO::getId);
        if (Boolean.TRUE.equals(reqVO.getUnqualifiedOnly())) {
            // 与库存服务 resolvePieceQualityStatus 一致：仅显式 OK 为合格，历史空值仍为 NG。
            query.and(quality -> quality.isNull(HcNgInventoryPieceDO::getQualityResult)
                    .or().ne(HcNgInventoryPieceDO::getQualityResult, "OK"));
            query.in(HcNgInventoryPieceDO::getStatus,
                    List.of("WAIT_SHELF", "WAIT_FREEZE_SHELF", "STORED", "FROZEN"));
        } else if (!Boolean.TRUE.equals(reqVO.getIncludeScrapped()) && reqVO.getStatus() == null) {
            query.in(HcNgInventoryPieceDO::getStatus, List.of("STORED", "FROZEN"));
        }
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper.like(HcNgInventoryPieceDO::getPieceNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceParentBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourcePlanNo, reqVO.getKeyword()));
        }
        return selectPage(reqVO, query);
    }

    default List<HcNgInventoryPieceDO> selectWaitShelfListForGroup(NgPiecePageReqVO reqVO, String status) {
        LambdaQueryWrapperX<HcNgInventoryPieceDO> query = new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getStatus, status)
                .eqIfPresent(HcNgInventoryPieceDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(HcNgInventoryPieceDO::getSourcePlanOperationId, reqVO.getSourcePlanOperationId())
                .eqIfPresent(HcNgInventoryPieceDO::getFreezeInstructionId, reqVO.getFreezeInstructionId())
                .eq(HcNgInventoryPieceDO::getDeleted, false);
        query.orderByAsc(HcNgInventoryPieceDO::getSourcePlanNo)
                .orderByAsc(HcNgInventoryPieceDO::getProcessType)
                .orderByAsc(HcNgInventoryPieceDO::getSourceParentBatchNo)
                .orderByAsc(HcNgInventoryPieceDO::getSourceBatchNo)
                .orderByAsc(HcNgInventoryPieceDO::getPieceNo)
                .orderByAsc(HcNgInventoryPieceDO::getId);
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper.like(HcNgInventoryPieceDO::getPieceNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceParentBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourcePlanNo, reqVO.getKeyword()));
        }
        return selectList(query);
    }

    default List<HcNgInventoryPieceDO> selectInventoryListForGroup(NgPiecePageReqVO reqVO) {
        LambdaQueryWrapperX<HcNgInventoryPieceDO> query = new LambdaQueryWrapperX<HcNgInventoryPieceDO>()
                .eqIfPresent(HcNgInventoryPieceDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(HcNgInventoryPieceDO::getSourcePlanOperationId, reqVO.getSourcePlanOperationId())
                .eqIfPresent(HcNgInventoryPieceDO::getCurrentWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(HcNgInventoryPieceDO::getPadType, reqVO.getPadType())
                .eqIfPresent(HcNgInventoryPieceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcNgInventoryPieceDO::getCurrentLocationCode, reqVO.getLocationKey())
                .eq(HcNgInventoryPieceDO::getDeleted, false);
        if (!Boolean.TRUE.equals(reqVO.getIncludeScrapped()) && reqVO.getStatus() == null) {
            query.in(HcNgInventoryPieceDO::getStatus, List.of("STORED", "FROZEN"));
        }
        query.orderByAsc(HcNgInventoryPieceDO::getSourcePlanNo)
                .orderByAsc(HcNgInventoryPieceDO::getProcessType)
                .orderByAsc(HcNgInventoryPieceDO::getSourceParentBatchNo)
                .orderByAsc(HcNgInventoryPieceDO::getSourceBatchNo)
                .orderByAsc(HcNgInventoryPieceDO::getPieceNo)
                .orderByAsc(HcNgInventoryPieceDO::getId);
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper.like(HcNgInventoryPieceDO::getPieceNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceParentBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourcePlanNo, reqVO.getKeyword()));
        }
        return selectList(query);
    }
}
