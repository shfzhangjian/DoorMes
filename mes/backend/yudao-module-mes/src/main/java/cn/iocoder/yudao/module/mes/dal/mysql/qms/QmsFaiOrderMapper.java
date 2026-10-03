package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

@Mapper
public interface QmsFaiOrderMapper extends BaseMapperX<QmsFaiOrderDO> {
    @Select("SELECT * FROM mes_qms_fai_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    QmsFaiOrderDO selectCoaOrderForUpdate(@Param("id") Long id);


    default PageResult<QmsFaiOrderDO> selectPage(QmsFaiPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsFaiOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.likeIfPresent(QmsFaiOrderDO::getFaiNo, reqVO.getFaiNo())
                .likeIfPresent(QmsFaiOrderDO::getWorkOrderNo, reqVO.getWorkOrderNo())
                .likeIfPresent(QmsFaiOrderDO::getMachineCode, reqVO.getMachineCode())
                .likeIfPresent(QmsFaiOrderDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsFaiOrderDO::getProductModel, reqVO.getProductModel())
                .likeIfPresent(QmsFaiOrderDO::getProductBatchNo, reqVO.getProductBatchNo())
                .likeIfPresent(QmsFaiOrderDO::getGlueBoardModel, reqVO.getGlueBoardModel())
                .likeIfPresent(QmsFaiOrderDO::getGlueBoardMaterialCode, reqVO.getGlueBoardMaterialCode())
                .likeIfPresent(QmsFaiOrderDO::getGluePlateBatchNo, reqVO.getGluePlateBatchNo())
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, reqVO.getSourceModule())
                .eqIfPresent(QmsFaiOrderDO::getSubmissionType, reqVO.getSubmissionType())
                .eqIfPresent(QmsFaiOrderDO::getWetSampleType, reqVO.getWetSampleType())
                .likeIfPresent(QmsFaiOrderDO::getSubmitterName, reqVO.getSubmitterName())
                .eqIfPresent(QmsFaiOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsFaiOrderDO::getJudgment, reqVO.getJudgment())
                .eqIfPresent(QmsFaiOrderDO::getRecheckFlag, reqVO.getRecheckFlag())
                .eqIfPresent(QmsFaiOrderDO::getRetentionStatus, reqVO.getRetentionStatus())
                .eqIfPresent(QmsFaiOrderDO::getRetentionDestroyStatus, reqVO.getRetentionDestroyStatus())
                .eqIfPresent(QmsFaiOrderDO::getTriggerReason, reqVO.getTriggerReason())
                .betweenIfPresent(QmsFaiOrderDO::getQaTime, reqVO.getQaTime())
                .betweenIfPresent(QmsFaiOrderDO::getSubmissionTime, reqVO.getSubmissionTime())
                .betweenIfPresent(QmsFaiOrderDO::getInspectionTime, reqVO.getInspectionTime())
                .betweenIfPresent(QmsFaiOrderDO::getRetentionExpireTime, reqVO.getRetentionExpireTime())
                .and(StringUtils.hasText(reqVO.getExcludedSourceModule()), excludedWrapper -> excludedWrapper
                        .isNull(QmsFaiOrderDO::getSourceModule)
                        .or()
                        .ne(QmsFaiOrderDO::getSourceModule, reqVO.getExcludedSourceModule()));
        appendRetentionLedgerCondition(queryWrapper, reqVO);
        appendItemRecheckStatusCondition(queryWrapper, reqVO.getItemRecheckStatusFilter());
        appendFaiLedgerProcessCondition(queryWrapper, reqVO.getProcessCategory());
        return selectPage(reqVO, queryWrapper.orderByDesc(QmsFaiOrderDO::getId));
    }

    default void appendRetentionLedgerCondition(LambdaQueryWrapperX<QmsFaiOrderDO> wrapper,
                                                QmsFaiPageReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(reqVO.getRetentionActiveOnly())) {
            wrapper.and(active -> active
                    .isNull(QmsFaiOrderDO::getRetentionExpireTime)
                    .or()
                    .gt(QmsFaiOrderDO::getRetentionExpireTime, now));
            wrapper.and(notDestroyed -> notDestroyed
                    .isNull(QmsFaiOrderDO::getRetentionDestroyStatus)
                    .or()
                    .ne(QmsFaiOrderDO::getRetentionDestroyStatus, "DESTROYED"));
        }
        if (Boolean.TRUE.equals(reqVO.getRetentionExpiredOnly())) {
            wrapper.isNotNull(QmsFaiOrderDO::getRetentionExpireTime)
                    .le(QmsFaiOrderDO::getRetentionExpireTime, now);
        }
    }

    default void appendItemRecheckStatusCondition(LambdaQueryWrapperX<QmsFaiOrderDO> wrapper,
                                                  String itemRecheckStatusFilter) {
        if (!StringUtils.hasText(itemRecheckStatusFilter)) {
            return;
        }
        String normalized = itemRecheckStatusFilter.trim().toUpperCase(Locale.ROOT);
        if (!"WAIT_RECHECK".equals(normalized) && !"WAIT_AUDIT".equals(normalized)) {
            return;
        }
        wrapper.exists("""
                SELECT 1
                FROM mes_qms_fai_item_group_audit fga
                WHERE fga.deleted = 0
                  AND fga.fai_id = mes_qms_fai_order.id
                  AND fga.item_recheck_status = '%s'
                """.formatted(normalized));
    }

    default void appendFaiLedgerProcessCondition(LambdaQueryWrapperX<QmsFaiOrderDO> wrapper,
                                                 String processCategory) {
        if (!StringUtils.hasText(processCategory)) {
            return;
        }
        List<String> processCategories = new ArrayList<>();
        List<String> operationCodes = new ArrayList<>();
        List<String> operationNames = new ArrayList<>();
        appendFaiLedgerProcessAliases(processCategory, processCategories, operationCodes, operationNames);
        wrapper.and(query -> {
            boolean appended = false;
            if (!processCategories.isEmpty()) {
                query.in(QmsFaiOrderDO::getProcessCategory, processCategories);
                appended = true;
            }
            if (!operationCodes.isEmpty()) {
                if (appended) {
                    query.or();
                }
                query.in(QmsFaiOrderDO::getOperationCode, operationCodes);
                appended = true;
            }
            if (!operationNames.isEmpty()) {
                if (appended) {
                    query.or();
                }
                query.in(QmsFaiOrderDO::getOperationName, operationNames);
            }
        });
    }

    default void appendFaiLedgerProcessAliases(String processCategory,
                                               List<String> processCategories,
                                               List<String> operationCodes,
                                               List<String> operationNames) {
        String text = processCategory.trim();
        String upperText = text.toUpperCase(Locale.ROOT);
        addIfAbsent(processCategories, text);
        addIfAbsent(operationCodes, text);
        if (containsChinese(text)) {
            addIfAbsent(operationNames, text);
        }
        if ("WET".equals(upperText) || "WC-COAT".equals(upperText) || "湿法".equals(text)) {
            addIfAbsent(processCategories, "WET");
            addIfAbsent(operationCodes, "WC-COAT");
            addIfAbsent(operationNames, "湿法");
        } else if ("GRINDING".equals(upperText) || "ROUGH_GRINDING".equals(upperText)
                || "WC-GRIND".equals(upperText) || "磨皮".equals(text)) {
            addIfAbsent(processCategories, "ROUGH_GRINDING");
            addIfAbsent(processCategories, "GRINDING");
            addIfAbsent(operationCodes, "WC-GRIND");
            addIfAbsent(operationNames, "磨皮");
        } else if ("ADHESIVE".equals(upperText) || "ADHESIVE1".equals(upperText)
                || "GLUE_1".equals(upperText) || "WC-ADH1".equals(upperText) || "粘胶1".equals(text)) {
            addIfAbsent(processCategories, "ADHESIVE");
            addIfAbsent(processCategories, "ADHESIVE1");
            addIfAbsent(processCategories, "GLUE_1");
            addIfAbsent(operationCodes, "ADHESIVE");
            addIfAbsent(operationCodes, "WC-ADH1");
            addIfAbsent(operationCodes, "GLUE_1");
            addIfAbsent(operationNames, "粘胶1");
        } else if ("PRESS_SLOT".equals(upperText) || "GROOVING".equals(upperText)
                || "WC-GROOVE".equals(upperText) || "压槽".equals(text)) {
            addIfAbsent(processCategories, "PRESS_SLOT");
            addIfAbsent(processCategories, "GROOVING");
            addIfAbsent(operationCodes, "WC-GROOVE");
            addIfAbsent(operationCodes, "GROOVING");
            addIfAbsent(operationNames, "压槽");
        } else if ("ADHESIVE2".equals(upperText) || "ADHESIVE_2".equals(upperText)
                || "GLUE_2".equals(upperText) || "BACK_GLUE".equals(upperText)
                || "WC-ADH2".equals(upperText) || "粘胶2".equals(text)) {
            addIfAbsent(processCategories, "ADHESIVE2");
            addIfAbsent(processCategories, "ADHESIVE_2");
            addIfAbsent(processCategories, "BACK_GLUE");
            addIfAbsent(processCategories, "GLUE_2");
            addIfAbsent(operationCodes, "ADHESIVE2");
            addIfAbsent(operationCodes, "WC-ADH2");
            addIfAbsent(operationCodes, "GLUE_2");
            addIfAbsent(operationNames, "粘胶2");
        } else if ("FINAL".equals(upperText) || "FINAL_PRODUCTS".equals(upperText) || "成品".equals(text)) {
            addIfAbsent(processCategories, "FINAL_PRODUCTS");
            addIfAbsent(processCategories, "FINAL");
            addIfAbsent(operationCodes, "FINAL_PRODUCTS");
            addIfAbsent(operationCodes, "FINAL");
            addIfAbsent(operationNames, "成品");
        }
    }

    default boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    default void addIfAbsent(List<String> values, String value) {
        if (StringUtils.hasText(value) && !values.contains(value)) {
            values.add(value);
        }
    }

    default QmsFaiOrderDO selectByFaiNo(String faiNo, Long excludeId) {
        return selectByFaiNo(faiNo, excludeId, null);
    }

    default QmsFaiOrderDO selectByFaiNo(String faiNo, Long excludeId, String sourceModule) {
        return selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getFaiNo, faiNo)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .neIfPresent(QmsFaiOrderDO::getId, excludeId));
    }

    default List<QmsFaiOrderDO> selectListByStatuses(Collection<String> statuses) {
        return selectListByStatuses(statuses, null, null);
    }

    default List<QmsFaiOrderDO> selectListByStatuses(Collection<String> statuses, String sourceModule,
                                                     String excludedSourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .and(StringUtils.hasText(excludedSourceModule), wrapper -> wrapper
                        .isNull(QmsFaiOrderDO::getSourceModule)
                        .or()
                        .ne(QmsFaiOrderDO::getSourceModule, excludedSourceModule))
                .orderByAsc(QmsFaiOrderDO::getCreateTime));
    }

    default QmsFaiOrderDO selectLatestGlueBoardFaiToday(Long glueBoardStockId, String glueBoardBatchNo,
                                                        String glueBoardModel, LocalDateTime dayStart,
                                                        LocalDateTime nextDayStart) {
        return selectLatestGlueBoardFaiToday(glueBoardStockId, glueBoardBatchNo, glueBoardModel,
                null, null, dayStart, nextDayStart);
    }

    default QmsFaiOrderDO selectLatestGlueBoardFaiToday(Long glueBoardStockId, String glueBoardBatchNo,
                                                        String glueBoardModel, String processCategory,
                                                        String operationNameKeyword, LocalDateTime dayStart,
                                                        LocalDateTime nextDayStart) {
        return selectLatestGlueBoardFaiToday(null, null, glueBoardStockId, glueBoardBatchNo, glueBoardModel,
                processCategory, operationNameKeyword, dayStart, nextDayStart);
    }

    default QmsFaiOrderDO selectLatestGlueBoardFaiToday(Long planOrderId, String sourceReportNo,
                                                        Long glueBoardStockId, String glueBoardBatchNo,
                                                        String glueBoardModel, String processCategory,
                                                        String operationNameKeyword, LocalDateTime dayStart,
                                                        LocalDateTime nextDayStart) {
        LambdaQueryWrapperX<QmsFaiOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFaiOrderDO::getSourceModule, "GLUE_BOARD_FAI")
                .eqIfPresent(QmsFaiOrderDO::getPlanOrderId, planOrderId)
                .eqIfPresent(QmsFaiOrderDO::getSourceReportNo, sourceReportNo)
                .ge(QmsFaiOrderDO::getSubmissionTime, dayStart)
                .lt(QmsFaiOrderDO::getSubmissionTime, nextDayStart);
        if (glueBoardStockId != null) {
            wrapper.eq(QmsFaiOrderDO::getGlueBoardStockId, glueBoardStockId);
        } else {
            wrapper.eqIfPresent(QmsFaiOrderDO::getGluePlateBatchNo, glueBoardBatchNo)
                    .eqIfPresent(QmsFaiOrderDO::getGlueBoardModel, glueBoardModel);
        }
        appendGlueBoardProcessCondition(wrapper, processCategory, operationNameKeyword);
        return selectOne(wrapper.orderByDesc(QmsFaiOrderDO::getId).last("LIMIT 1"));
    }

    default QmsFaiOrderDO selectLatestGlueBoardFai(Long glueBoardStockId, String glueBoardBatchNo,
                                                   String glueBoardModel, String processCategory,
                                                   String operationNameKeyword) {
        return selectLatestGlueBoardFai(null, null, glueBoardStockId, glueBoardBatchNo, glueBoardModel,
                processCategory, operationNameKeyword);
    }

    default QmsFaiOrderDO selectLatestGlueBoardFai(Long planOrderId, String sourceReportNo,
                                                   Long glueBoardStockId, String glueBoardBatchNo,
                                                   String glueBoardModel, String processCategory,
                                                   String operationNameKeyword) {
        LambdaQueryWrapperX<QmsFaiOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFaiOrderDO::getSourceModule, "GLUE_BOARD_FAI")
                .eqIfPresent(QmsFaiOrderDO::getPlanOrderId, planOrderId)
                .eqIfPresent(QmsFaiOrderDO::getSourceReportNo, sourceReportNo);
        if (glueBoardStockId != null) {
            wrapper.eq(QmsFaiOrderDO::getGlueBoardStockId, glueBoardStockId);
        } else {
            wrapper.eqIfPresent(QmsFaiOrderDO::getGluePlateBatchNo, glueBoardBatchNo)
                    .eqIfPresent(QmsFaiOrderDO::getGlueBoardModel, glueBoardModel);
        }
        appendGlueBoardProcessCondition(wrapper, processCategory, operationNameKeyword);
        return selectOne(wrapper.orderByDesc(QmsFaiOrderDO::getId).last("LIMIT 1"));
    }

    default void appendGlueBoardProcessCondition(LambdaQueryWrapperX<QmsFaiOrderDO> wrapper,
                                                 String processCategory,
                                                 String operationNameKeyword) {
        if (!StringUtils.hasText(processCategory) && !StringUtils.hasText(operationNameKeyword)) {
            return;
        }
        wrapper.and(query -> {
            boolean hasProcessCategory = StringUtils.hasText(processCategory);
            if (hasProcessCategory) {
                query.eq(QmsFaiOrderDO::getOperationCode, processCategory)
                        .or()
                        .eq(QmsFaiOrderDO::getProcessCategory, processCategory);
            }
            if (StringUtils.hasText(operationNameKeyword)) {
                if (hasProcessCategory) {
                    query.or();
                }
                query.like(QmsFaiOrderDO::getOperationName, operationNameKeyword);
            }
        });
    }

    /**
     * 查询报工时点前最新一笔已得出结论的胶板检验。
     *
     * <p>胶板发生 NG 后允许通过后续 OK 复检解除锁定，因此不能仅按历史最早 NG 判断。</p>
     */
    default QmsFaiOrderDO selectLatestCompletedGlueBoardFaiBefore(Long glueBoardStockId,
                                                                   LocalDateTime reportTime) {
        if (glueBoardStockId == null || reportTime == null) {
            return null;
        }
        LambdaQueryWrapperX<QmsFaiOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFaiOrderDO::getSourceModule, "GLUE_BOARD_FAI")
                .eq(QmsFaiOrderDO::getGlueBoardStockId, glueBoardStockId)
                .in(QmsFaiOrderDO::getJudgment, "OK", "NG")
                .le(QmsFaiOrderDO::getInspectionTime, reportTime);
        return selectOne(wrapper.orderByDesc(QmsFaiOrderDO::getInspectionTime)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFaiOrderDO selectLatestBySourceReportId(Long sourceReportId) {
        return selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportId, sourceReportId)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsFaiOrderDO> selectListBySourceReportIds(Collection<Long> sourceReportIds,
                                                            String sourceModule,
                                                            String processCategory) {
        if (sourceReportIds == null || sourceReportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .in(QmsFaiOrderDO::getSourceReportId, sourceReportIds)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .eqIfPresent(QmsFaiOrderDO::getProcessCategory, processCategory)
                .eq(QmsFaiOrderDO::getDeleted, false)
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default QmsFaiOrderDO selectLatestBySourceReportNo(String sourceReportNo, String sourceModule) {
        return selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportNo, sourceReportNo)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsFaiOrderDO> selectListBySourceReportNoAndSubmissionTime(String sourceReportNo, String sourceModule,
                                                                            LocalDateTime dayStart,
                                                                            LocalDateTime nextDayStart) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportNo, sourceReportNo)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .ge(QmsFaiOrderDO::getSubmissionTime, dayStart)
                .lt(QmsFaiOrderDO::getSubmissionTime, nextDayStart)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListBySourceReportNo(String sourceReportNo, String sourceModule,
                                                           Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportNo, sourceReportNo)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .inIfPresent(QmsFaiOrderDO::getStatus, statuses)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectAllBySourceReportNoAsc(String sourceReportNo, String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportNo, sourceReportNo)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByAsc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListBySourceReportNoPrefix(String sourceReportNoPrefix, String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .likeIfPresent(QmsFaiOrderDO::getSourceReportNo, sourceReportNoPrefix)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    @Select("""
            <script>
            SELECT *
            FROM mes_qms_fai_order
            WHERE deleted = 0
              AND source_module = #{sourceModule}
              AND source_report_no LIKE '%-COA-%'
              AND (
                <foreach collection="segmentBatchNos" item="segmentBatchNo" separator=" OR ">
                  product_batch_no LIKE CONCAT(#{segmentBatchNo}, '%')
                </foreach>
              )
            ORDER BY update_time DESC, submission_time DESC, id DESC
            </script>
            """)
    List<QmsFaiOrderDO> selectCoaListBySegmentBatchNos(@Param("sourceModule") String sourceModule,
                                                       @Param("segmentBatchNos") Collection<String> segmentBatchNos);

    default List<QmsFaiOrderDO> selectPackagingCoaListBySegmentBatchNos(Collection<String> segmentBatchNos) {
        if (segmentBatchNos == null || segmentBatchNos.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceModule, "PACKAGING_COA")
                .in(QmsFaiOrderDO::getProductBatchNo, segmentBatchNos)
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectPressSlotInspectionEvents(String sourceModule,
                                                                LocalDateTime dayStart,
                                                                LocalDateTime nextDayStart) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .and(wrapper -> wrapper
                        .eq(QmsFaiOrderDO::getProcessCategory, "PRESS_SLOT")
                        .or()
                        .isNull(QmsFaiOrderDO::getProcessCategory))
                .and(wrapper -> wrapper
                        .ge(QmsFaiOrderDO::getSubmissionTime, dayStart)
                        .lt(QmsFaiOrderDO::getSubmissionTime, nextDayStart)
                        .or()
                        .ge(QmsFaiOrderDO::getCreateTime, dayStart)
                        .lt(QmsFaiOrderDO::getCreateTime, nextDayStart)
                        .or()
                        .ge(QmsFaiOrderDO::getUpdateTime, dayStart)
                        .lt(QmsFaiOrderDO::getUpdateTime, nextDayStart))
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListByWorkOrderNo(String workOrderNo, Collection<String> statuses) {
        return selectListByWorkOrderNo(workOrderNo, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListByWorkOrderNo(String workOrderNo, Collection<String> statuses,
                                                        String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getWorkOrderNo, workOrderNo)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListByProductBatchNo(String productBatchNo, Collection<String> statuses) {
        return selectListByProductBatchNo(productBatchNo, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListByProductBatchNo(String productBatchNo, Collection<String> statuses,
                                                           String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getProductBatchNo, productBatchNo)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListByProductBatchNos(Collection<String> productBatchNos,
                                                            String sourceModule,
                                                            String processCategory) {
        if (productBatchNos == null || productBatchNos.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .in(QmsFaiOrderDO::getProductBatchNo, productBatchNos)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .eqIfPresent(QmsFaiOrderDO::getProcessCategory, processCategory)
                .eq(QmsFaiOrderDO::getDeleted, false)
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    /**
     * 查询已占用指定片号的压槽首检单。
     *
     * <p>过程加检和异常放行单不占用首检样片；已逻辑删除的历史单据不参与校验。</p>
     */
    default QmsFaiOrderDO selectAnyPressSlotFirstInspectionByProductBatchNo(String sourceModule,
                                                                             String productBatchNo) {
        if (!StringUtils.hasText(productBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .eq(QmsFaiOrderDO::getProductBatchNo, productBatchNo.trim())
                .eq(QmsFaiOrderDO::getProcessCategory, "PRESS_SLOT")
                .and(wrapper -> wrapper
                        .isNull(QmsFaiOrderDO::getSourceReportNo)
                        .or()
                        .notLike(QmsFaiOrderDO::getSourceReportNo, "-PROCESS-CHECK-"))
                .and(wrapper -> wrapper
                        .isNull(QmsFaiOrderDO::getSourceReportNo)
                        .or()
                        .notLike(QmsFaiOrderDO::getSourceReportNo, "-ABNORMAL-RELEASE-"))
                .eq(QmsFaiOrderDO::getDeleted, false)
                .ne(QmsFaiOrderDO::getStatus, "CANCELED")
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFaiOrderDO selectLatestTerminalPressSlotFirstInspection(String sourceModule,
                                                                       Long planOrderId,
                                                                       String productBatchNo) {
        if (!StringUtils.hasText(productBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .eqIfPresent(QmsFaiOrderDO::getPlanOrderId, planOrderId)
                .eq(QmsFaiOrderDO::getProductBatchNo, productBatchNo)
                .eq(QmsFaiOrderDO::getProcessCategory, "PRESS_SLOT")
                .notLike(QmsFaiOrderDO::getSourceReportNo, "-PROCESS-CHECK-")
                .notLike(QmsFaiOrderDO::getSourceReportNo, "-ABNORMAL-RELEASE-")
                .and(wrapper -> wrapper
                        .and(ok -> ok
                                .eq(QmsFaiOrderDO::getStatus, "COMPLETED")
                                .eq(QmsFaiOrderDO::getJudgment, "OK"))
                        .or()
                        .eq(QmsFaiOrderDO::getJudgment, "NG")
                        .or()
                        .eq(QmsFaiOrderDO::getStatus, "REJECTED"))
                .orderByDesc(QmsFaiOrderDO::getSubmissionTime)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsFaiOrderDO> selectListByMachineCode(String machineCode, Collection<String> statuses) {
        return selectListByMachineCode(machineCode, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListByMachineCode(String machineCode, Collection<String> statuses,
                                                        String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getMachineCode, machineCode)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListByMaterialCode(String materialCode, Collection<String> statuses) {
        return selectListByMaterialCode(materialCode, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListByMaterialCode(String materialCode, Collection<String> statuses,
                                                         String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getMaterialCode, materialCode)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListByProductModel(String productModel, Collection<String> statuses) {
        return selectListByProductModel(productModel, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListByProductModel(String productModel, Collection<String> statuses,
                                                         String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getProductModel, productModel)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default List<QmsFaiOrderDO> selectListBySheetTemplateCode(String sheetTemplateCode, Collection<String> statuses) {
        return selectListBySheetTemplateCode(sheetTemplateCode, statuses, null);
    }

    default List<QmsFaiOrderDO> selectListBySheetTemplateCode(String sheetTemplateCode, Collection<String> statuses,
                                                              String sourceModule) {
        return selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSheetTemplateCode, sheetTemplateCode)
                .in(QmsFaiOrderDO::getStatus, statuses)
                .eqIfPresent(QmsFaiOrderDO::getSourceModule, sourceModule)
                .orderByDesc(QmsFaiOrderDO::getUpdateTime)
                .orderByDesc(QmsFaiOrderDO::getId));
    }

    default void updateAuditNotifyTime(Long id, LocalDateTime auditNotifyTime) {
        update(null, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getId, id)
                .set(QmsFaiOrderDO::getAuditNotifyTime, auditNotifyTime));
    }

    /**
     * 仅撤回品质尚未扫码、录入或提交的待检单，条件更新避免并发下误撤回已处理单据。
     */
    default boolean cancelUntouchedPendingById(Long id, String withdrawReason, String withdrawer) {
        return update(null, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getId, id)
                .eq(QmsFaiOrderDO::getDeleted, false)
                .eq(QmsFaiOrderDO::getStatus, "PENDING")
                .isNull(QmsFaiOrderDO::getOperatorTime)
                .isNull(QmsFaiOrderDO::getQaTime)
                .isNull(QmsFaiOrderDO::getLastSaveTime)
                .isNull(QmsFaiOrderDO::getLastScanTime)
                .set(QmsFaiOrderDO::getStatus, "CANCELED")
                .set(QmsFaiOrderDO::getJudgment, "PENDING")
                .set(QmsFaiOrderDO::getLastReturnReason, withdrawReason)
                .set(QmsFaiOrderDO::getUpdater, withdrawer)
                .set(QmsFaiOrderDO::getUpdateTime, LocalDateTime.now())) > 0;
    }

    default void clearAuditNotifyTime(Long id) {
        updateAuditNotifyTime(id, null);
    }
}
