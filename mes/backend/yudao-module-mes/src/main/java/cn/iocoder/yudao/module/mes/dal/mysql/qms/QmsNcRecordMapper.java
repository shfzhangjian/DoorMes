package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcRecordMapper extends BaseMapperX<QmsNcRecordDO> {

    String SOURCE_TYPE_RAW_MATERIAL = "RAW_MATERIAL";
    Collection<String> TERMINAL_STATUSES = Arrays.asList("CLOSED", "CANCELLED");

    default PageResult<QmsNcRecordDO> selectPage(QmsNcRecordPageReqVO reqVO,
                                                 Long currentUserId,
                                                 Collection<Long> participatedNcRecordIds,
                                                 Collection<Long> pendingReviewNcRecordIds,
                                                 Collection<Long> pendingNotifyNcRecordIds,
                                                 boolean rawMaterialQualityConfirmUser,
                                                 boolean productQualityConfirmUser) {
        LambdaQueryWrapperX<QmsNcRecordDO> wrapper = new LambdaQueryWrapperX<QmsNcRecordDO>()
                .likeIfPresent(QmsNcRecordDO::getNcNo, reqVO.getNcNo())
                .likeIfPresent(QmsNcRecordDO::getLotNo, reqVO.getLotNo())
                .likeIfPresent(QmsNcRecordDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsNcRecordDO::getMaterialName, reqVO.getMaterialName())
                .eqIfPresent(QmsNcRecordDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(QmsNcRecordDO::getSourceBizType, reqVO.getSourceBizType())
                .likeIfPresent(QmsNcRecordDO::getHappenDeptName, reqVO.getHappenDeptName())
                .eqIfPresent(QmsNcRecordDO::getRawMaterialAbnormalCategory, reqVO.getRawMaterialAbnormalCategory())
                .eqIfPresent(QmsNcRecordDO::getSubOrderId, reqVO.getSubOrderId())
                .eqIfPresent(QmsNcRecordDO::getDefectCode, reqVO.getDefectCode())
                .eqIfPresent(QmsNcRecordDO::getNcLevel, reqVO.getNcLevel())
                .eqIfPresent(QmsNcRecordDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsNcRecordDO::getMrbDecision, reqVO.getMrbDecision())
                .eqIfPresent(QmsNcRecordDO::getFinalDisposition, reqVO.getFinalDisposition())
                .betweenIfPresent(QmsNcRecordDO::getHappenTime, reqVO.getHappenTime());
        if (Boolean.TRUE.equals(reqVO.getInProgressOnly())) {
            wrapper.notIn(QmsNcRecordDO::getStatus, "DRAFT", "CLOSED", "CANCELLED");
        }
        if (StrUtil.isNotBlank(reqVO.getPackagingPieceNo())) {
            wrapper.apply("EXISTS (SELECT 1 FROM (" + QmsNcPackagingFlowMapper.RELATIONS
                    + ") packaging_scope WHERE packaging_scope.nc_record_id = mes_qms_nc_record.id"
                    + " AND packaging_scope.tenant_id = mes_qms_nc_record.tenant_id"
                    + " AND packaging_scope.piece_no = {0})", reqVO.getPackagingPieceNo().trim());
        }
        if (Boolean.TRUE.equals(reqVO.getExcludeRawMaterial())) {
            wrapper.and(source -> source.isNull(QmsNcRecordDO::getSourceType)
                    .or().ne(QmsNcRecordDO::getSourceType, SOURCE_TYPE_RAW_MATERIAL));
        }
        if (Boolean.TRUE.equals(reqVO.getLinkableExceptionOnly())) {
            wrapper.and(w -> w.isNull(QmsNcRecordDO::getRelatedExceptionNo)
                            .or().eq(QmsNcRecordDO::getRelatedExceptionNo, ""))
                    .isNull(QmsNcRecordDO::getRelatedExceptionId);
        }
        boolean hasMineScope = Boolean.TRUE.equals(reqVO.getPendingMine())
                || Boolean.TRUE.equals(reqVO.getDiscoveredMine())
                || Boolean.TRUE.equals(reqVO.getParticipatedMine());
        if (hasMineScope) {
            Long userId = currentUserId == null ? -1L : currentUserId;
            wrapper.and(scope -> {
                boolean appended = false;
                if (Boolean.TRUE.equals(reqVO.getPendingMine())) {
                    scope.and(pending -> {
                        pending.and(active -> active.notIn(QmsNcRecordDO::getStatus, TERMINAL_STATUSES)
                                .and(owner -> appendPendingOwnerScope(owner, userId, pendingReviewNcRecordIds,
                                        rawMaterialQualityConfirmUser, productQualityConfirmUser)));
                        if (CollUtil.isNotEmpty(pendingNotifyNcRecordIds)) {
                            pending.or().in(QmsNcRecordDO::getId, pendingNotifyNcRecordIds);
                        }
                    });
                    appended = true;
                }
                if (Boolean.TRUE.equals(reqVO.getDiscoveredMine())) {
                    if (appended) {
                        scope.or();
                    }
                    scope.eq(QmsNcRecordDO::getApplicantUserId, userId);
                    appended = true;
                }
                if (Boolean.TRUE.equals(reqVO.getParticipatedMine())) {
                    if (appended) {
                        scope.or();
                    }
                    if (CollUtil.isEmpty(participatedNcRecordIds)) {
                        scope.eq(QmsNcRecordDO::getId, -1L);
                    } else {
                        scope.in(QmsNcRecordDO::getId, participatedNcRecordIds);
                    }
                }
            });
        } else if ("todo".equals(reqVO.getTabType())) {
            Long handlerId = currentUserId == null ? -1L : currentUserId;
            wrapper.and(w -> {
                w.and(active -> active.notIn(QmsNcRecordDO::getStatus, TERMINAL_STATUSES)
                        .and(owner -> appendPendingOwnerScope(owner, handlerId, pendingReviewNcRecordIds,
                                rawMaterialQualityConfirmUser, productQualityConfirmUser)));
                if (CollUtil.isNotEmpty(pendingNotifyNcRecordIds)) {
                    w.or().in(QmsNcRecordDO::getId, pendingNotifyNcRecordIds);
                }
            });
        } else if ("initiated".equals(reqVO.getTabType())) {
            wrapper.eq(QmsNcRecordDO::getApplicantUserId, currentUserId == null ? -1L : currentUserId);
        } else if ("processed".equals(reqVO.getTabType())) {
            if (CollUtil.isEmpty(participatedNcRecordIds)) {
                wrapper.eq(QmsNcRecordDO::getId, -1L);
            } else {
                wrapper.in(QmsNcRecordDO::getId, participatedNcRecordIds);
            }
        }
        wrapper.orderByDesc(QmsNcRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default void appendPendingOwnerScope(LambdaQueryWrapper<QmsNcRecordDO> owner,
                                         Long userId,
                                         Collection<Long> pendingReviewNcRecordIds,
                                         boolean rawMaterialQualityConfirmUser,
                                         boolean productQualityConfirmUser) {
        owner.eq(QmsNcRecordDO::getCurrentHandlerUserId, userId);
        if (CollUtil.isNotEmpty(pendingReviewNcRecordIds)) {
            owner.or().in(QmsNcRecordDO::getId, pendingReviewNcRecordIds);
        }
        if (rawMaterialQualityConfirmUser) {
            owner.or().and(rawQuality -> rawQuality
                    .eq(QmsNcRecordDO::getSourceType, SOURCE_TYPE_RAW_MATERIAL)
                    .eq(QmsNcRecordDO::getStatus, "SUBMITTED"));
        }
        if (productQualityConfirmUser) {
            owner.or().and(productQuality -> productQuality
                    .and(source -> source.isNull(QmsNcRecordDO::getSourceType)
                            .or().ne(QmsNcRecordDO::getSourceType, SOURCE_TYPE_RAW_MATERIAL))
                    .eq(QmsNcRecordDO::getStatus, "SUBMITTED"));
        }
        owner.or().and(legacyDraft -> legacyDraft
                .in(QmsNcRecordDO::getStatus, "DRAFT", "RETURNED")
                .isNull(QmsNcRecordDO::getCurrentHandlerUserId)
                .eq(QmsNcRecordDO::getApplicantUserId, userId));
    }

    default QmsNcRecordDO selectByNcNo(String ncNo) {
        return selectOne(QmsNcRecordDO::getNcNo, ncNo);
    }

    default QmsNcRecordDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getProcessInstanceId, processInstanceId)
                .last("LIMIT 1"));
    }

    default QmsNcRecordDO selectLastByNcNoPrefix(String ncNoPrefix) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .select(QmsNcRecordDO::getId, QmsNcRecordDO::getNcNo)
                .likeRight(QmsNcRecordDO::getNcNo, ncNoPrefix)
                .orderByDesc(QmsNcRecordDO::getNcNo)
                .last("LIMIT 1"));
    }

    default QmsNcRecordDO selectLightById(Long id) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .select(QmsNcRecordDO::getId, QmsNcRecordDO::getNcNo, QmsNcRecordDO::getStatus)
                .eq(QmsNcRecordDO::getId, id)
                .last("LIMIT 1"));
    }

    default List<Long> selectIdsByLotNoLike(String lotNo) {
        if (StrUtil.isBlank(lotNo)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .select(QmsNcRecordDO::getId)
                .like(QmsNcRecordDO::getLotNo, lotNo.trim()))
                .stream()
                .map(QmsNcRecordDO::getId)
                .toList();
    }

    default QmsNcRecordDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default void clearCurrentHandler(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getCurrentHandlerUserId, null)
                .set(QmsNcRecordDO::getCurrentHandlerUserName, null));
    }

    default void clearStockDisposeResult(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getStockDisposeResult, null)
                .set(QmsNcRecordDO::getStockDisposeTime, null));
    }

    default void clearFinalFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getFinalDisposition, null)
                .set(QmsNcRecordDO::getFinalOpinion, null)
                .set(QmsNcRecordDO::getFinalDisposeDescription, null)
                .set(QmsNcRecordDO::getFinalApproverId, null)
                .set(QmsNcRecordDO::getFinalApproverName, null)
                .set(QmsNcRecordDO::getFinalApproveTime, null)
                .set(QmsNcRecordDO::getMrbDecision, "PENDING"));
    }

    default void clearContentConfirmFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getContentConfirmUserId, null)
                .set(QmsNcRecordDO::getContentConfirmUserName, null)
                .set(QmsNcRecordDO::getContentConfirmTime, null));
    }

    default void clearQualityConfirmFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getQualityConfirmUserId, null)
                .set(QmsNcRecordDO::getQualityConfirmUserName, null)
                .set(QmsNcRecordDO::getQualityConfirmTime, null));
    }

    default void clearStockDisposeFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getStockDisposeStatus, null)
                .set(QmsNcRecordDO::getStockDisposeQty, null)
                .set(QmsNcRecordDO::getStockDisposeUserId, null)
                .set(QmsNcRecordDO::getStockDisposeUserName, null)
                .set(QmsNcRecordDO::getStockDisposeResult, null)
                .set(QmsNcRecordDO::getStockDisposeTime, null));
    }

    default void clearCloseFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getCloseTime, null)
                .set(QmsNcRecordDO::getCloseUserId, null)
                .set(QmsNcRecordDO::getCloseUserName, null));
    }

    default void clearEffectConfirmFields(Long id) {
        update(new LambdaUpdateWrapper<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getId, id)
                .set(QmsNcRecordDO::getEffectConfirmResult, null)
                .set(QmsNcRecordDO::getEffectConfirmUserId, null)
                .set(QmsNcRecordDO::getEffectConfirmUserName, null)
                .set(QmsNcRecordDO::getEffectConfirmTime, null));
    }

    default QmsNcRecordDO selectLightBySourceBiz(String sourceBizType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .select(QmsNcRecordDO::getId, QmsNcRecordDO::getNcNo, QmsNcRecordDO::getStatus)
                .eq(QmsNcRecordDO::getSourceBizType, sourceBizType)
                .eq(QmsNcRecordDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    default QmsNcRecordDO selectNonCancelledLightBySourceBiz(String sourceBizType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .select(QmsNcRecordDO::getId, QmsNcRecordDO::getNcNo, QmsNcRecordDO::getStatus)
                .eq(QmsNcRecordDO::getSourceBizType, sourceBizType)
                .eq(QmsNcRecordDO::getSourceId, sourceId)
                .and(wrapper -> wrapper.ne(QmsNcRecordDO::getStatus, "CANCELLED")
                        .or().isNull(QmsNcRecordDO::getStatus))
                .orderByDesc(QmsNcRecordDO::getId)
                .last("LIMIT 1"));
    }

    default QmsNcRecordDO selectBySourceBiz(String sourceBizType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRecordDO>()
                .eq(QmsNcRecordDO::getSourceBizType, sourceBizType)
                .eq(QmsNcRecordDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }
}
