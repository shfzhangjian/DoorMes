package cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionMessagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcProductionInstructionMapper extends BaseMapperX<HcProductionInstructionDO> {

    default HcProductionInstructionDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .eq(HcProductionInstructionDO::getId, id)
                .last("FOR UPDATE"));
    }

    default HcProductionInstructionDO selectLatestExecutingChangeover(Long planId, Long planOperationId,
                                                                         String segmentBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .eq(HcProductionInstructionDO::getPlanId, planId)
                .eq(HcProductionInstructionDO::getPlanOperationId, planOperationId)
                .eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                .eq(HcProductionInstructionDO::getInstructionType, "CHANGEOVER")
                .eq(HcProductionInstructionDO::getExecuteStatus, "EXECUTING")
                .ne(HcProductionInstructionDO::getStatus, "REVOKED")
                .orderByDesc(HcProductionInstructionDO::getExecuteStartTime)
                .orderByDesc(HcProductionInstructionDO::getId)
                .last("LIMIT 1"));
    }

    default HcProductionInstructionDO selectLatestOpenChangeover(Long planId, Long planOperationId,
                                                                    String segmentBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .eq(HcProductionInstructionDO::getPlanId, planId)
                .eq(HcProductionInstructionDO::getPlanOperationId, planOperationId)
                .eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                .eq(HcProductionInstructionDO::getInstructionType, "CHANGEOVER")
                .in(HcProductionInstructionDO::getExecuteStatus, List.of("PENDING", "EXECUTING"))
                .ne(HcProductionInstructionDO::getStatus, "REVOKED")
                .orderByDesc(HcProductionInstructionDO::getExecuteStartTime)
                .orderByDesc(HcProductionInstructionDO::getId)
                .last("LIMIT 1"));
    }

    default HcProductionInstructionDO selectLatestEffectiveChangeover(Long planId, Long planOperationId,
                                                                        String segmentBatchNo) {
        return selectLatestEffectiveChangeover(planId, planOperationId, segmentBatchNo, false);
    }

    default HcProductionInstructionDO selectLatestEffectiveChangeoverForUpdate(Long planId, Long planOperationId,
                                                                                 String segmentBatchNo) {
        return selectLatestEffectiveChangeover(planId, planOperationId, segmentBatchNo, true);
    }

    private HcProductionInstructionDO selectLatestEffectiveChangeover(Long planId, Long planOperationId,
                                                                         String segmentBatchNo, boolean forUpdate) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .eq(HcProductionInstructionDO::getPlanId, planId)
                .eq(HcProductionInstructionDO::getPlanOperationId, planOperationId)
                .eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                .eq(HcProductionInstructionDO::getInstructionType, "CHANGEOVER")
                .in(HcProductionInstructionDO::getExecuteStatus, List.of("EXECUTING", "COMPLETED"))
                .ne(HcProductionInstructionDO::getStatus, "REVOKED")
                .last("ORDER BY CASE WHEN execute_status = 'EXECUTING' THEN 0 ELSE 1 END, "
                        + "execute_start_time DESC, id DESC LIMIT 1" + (forUpdate ? " FOR UPDATE" : "")));
    }

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM mes_pp_production_instruction i
            WHERE i.deleted = 0
              <if test="req.planOperationId != null">
                AND i.plan_operation_id = #{req.planOperationId}
              </if>
              <if test="req.planId != null">
                AND i.plan_id = #{req.planId}
              </if>
              <if test="req.planNo != null and req.planNo != ''">
                AND i.plan_no = #{req.planNo}
              </if>
              <if test="req.batchNo != null and req.batchNo != ''">
                AND i.batch_no = #{req.batchNo}
              </if>
              <if test="req.instructionType != null and req.instructionType != ''">
                AND i.instruction_type = #{req.instructionType}
              </if>
              <if test="req.status != null and req.status != ''">
                AND i.status = #{req.status}
              </if>
              <if test="(req.processCode != null and req.processCode != '')
                  or (req.operationCode != null and req.operationCode != '')
                  or (req.processName != null and req.processName != '')
                  or (req.operationName != null and req.operationName != '')">
                <trim prefix="AND (" suffix=")" prefixOverrides="OR">
                  <if test="req.processCode != null and req.processCode != ''">
                    OR i.process_code = #{req.processCode}
                    OR i.operation_code = #{req.processCode}
                  </if>
                  <if test="req.operationCode != null and req.operationCode != ''">
                    OR i.operation_code = #{req.operationCode}
                    OR i.process_code = #{req.operationCode}
                  </if>
                  <if test="req.processName != null and req.processName != ''">
                    OR i.process_name LIKE CONCAT('%', #{req.processName}, '%')
                    OR i.operation_name LIKE CONCAT('%', #{req.processName}, '%')
                  </if>
                  <if test="req.operationName != null and req.operationName != ''">
                    OR i.operation_name LIKE CONCAT('%', #{req.operationName}, '%')
                    OR i.process_name LIKE CONCAT('%', #{req.operationName}, '%')
                  </if>
                </trim>
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (i.instruction_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.operation_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.process_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.instruction_content LIKE CONCAT('%', #{req.keyword}, '%'))
              </if>
            </script>
            """)
    Long selectMessageCount(@Param("req") HcProductionInstructionMessagePageReqVO reqVO);

    @Select("""
            <script>
            SELECT
              i.id,
              i.instruction_no AS instructionNo,
              i.instruction_batch_no AS instructionBatchNo,
              i.parent_instruction_id AS parentInstructionId,
              i.plan_id AS planId,
              i.plan_no AS planNo,
              i.plan_operation_id AS planOperationId,
              i.process_id AS processId,
              i.process_code AS processCode,
              i.process_name AS processName,
              i.operation_code AS operationCode,
              i.operation_name AS operationName,
              i.batch_no AS batchNo,
              i.production_batch_no AS productionBatchNo,
              i.segment_batch_no AS segmentBatchNo,
              i.instruction_type AS instructionType,
              i.scope_type AS scopeType,
              i.instruction_content AS instructionContent,
              i.before_material_code AS beforeMaterialCode,
              i.target_material_code AS targetMaterialCode,
              i.before_model_code AS beforeModelCode,
              i.target_model_code AS targetModelCode,
              i.target_qty AS targetQty,
              i.completed_qty AS completedQty,
              i.execute_status AS executeStatus,
              i.auto_restore_flag AS autoRestoreFlag,
              i.execute_user_id AS executeUserId,
              i.execute_user_name AS executeUserName,
              i.execute_start_time AS executeStartTime,
              i.execute_end_time AS executeEndTime,
              i.issuer_id AS issuerId,
              i.issuer_name AS issuerName,
              i.issued_time AS issuedTime,
              i.confirmer_id AS confirmerId,
              i.confirmer_name AS confirmerName,
              i.confirm_time AS confirmTime,
              i.revoked_by AS revokedBy,
              i.revoked_by_name AS revokedByName,
              i.revoked_time AS revokedTime,
              i.revoke_reason AS revokeReason,
              i.status,
              i.remark,
              i.create_time AS createTime,
              i.update_time AS updateTime,
              NULL AS recipientNotifyStatus,
              NULL AS recipientNotifyTime,
              FALSE AS unread
            FROM mes_pp_production_instruction i
            WHERE i.deleted = 0
              <if test="req.planOperationId != null">
                AND i.plan_operation_id = #{req.planOperationId}
              </if>
              <if test="req.planId != null">
                AND i.plan_id = #{req.planId}
              </if>
              <if test="req.planNo != null and req.planNo != ''">
                AND i.plan_no = #{req.planNo}
              </if>
              <if test="req.batchNo != null and req.batchNo != ''">
                AND i.batch_no = #{req.batchNo}
              </if>
              <if test="req.instructionType != null and req.instructionType != ''">
                AND i.instruction_type = #{req.instructionType}
              </if>
              <if test="req.status != null and req.status != ''">
                AND i.status = #{req.status}
              </if>
              <if test="(req.processCode != null and req.processCode != '')
                  or (req.operationCode != null and req.operationCode != '')
                  or (req.processName != null and req.processName != '')
                  or (req.operationName != null and req.operationName != '')">
                <trim prefix="AND (" suffix=")" prefixOverrides="OR">
                  <if test="req.processCode != null and req.processCode != ''">
                    OR i.process_code = #{req.processCode}
                    OR i.operation_code = #{req.processCode}
                  </if>
                  <if test="req.operationCode != null and req.operationCode != ''">
                    OR i.operation_code = #{req.operationCode}
                    OR i.process_code = #{req.operationCode}
                  </if>
                  <if test="req.processName != null and req.processName != ''">
                    OR i.process_name LIKE CONCAT('%', #{req.processName}, '%')
                    OR i.operation_name LIKE CONCAT('%', #{req.processName}, '%')
                  </if>
                  <if test="req.operationName != null and req.operationName != ''">
                    OR i.operation_name LIKE CONCAT('%', #{req.operationName}, '%')
                    OR i.process_name LIKE CONCAT('%', #{req.operationName}, '%')
                  </if>
                </trim>
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (i.instruction_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.operation_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.process_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR i.instruction_content LIKE CONCAT('%', #{req.keyword}, '%'))
              </if>
            ORDER BY i.issued_time DESC, i.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<HcProductionInstructionRespVO> selectMessagePage(@Param("req") HcProductionInstructionMessagePageReqVO reqVO,
                                                          @Param("limit") Integer limit,
                                                          @Param("offset") Integer offset);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM mes_pp_production_instruction i
            WHERE i.deleted = 0
              AND i.status != 'REVOKED'
              <if test="req.planOperationId != null">
                AND i.plan_operation_id = #{req.planOperationId}
              </if>
              <if test="req.planId != null">
                AND i.plan_id = #{req.planId}
              </if>
              <if test="req.planNo != null and req.planNo != ''">
                AND i.plan_no = #{req.planNo}
              </if>
              <if test="req.batchNo != null and req.batchNo != ''">
                AND i.batch_no = #{req.batchNo}
              </if>
              <if test="(req.processCode != null and req.processCode != '')
                  or (req.operationCode != null and req.operationCode != '')
                  or (req.processName != null and req.processName != '')
                  or (req.operationName != null and req.operationName != '')">
                <trim prefix="AND (" suffix=")" prefixOverrides="OR">
                  <if test="req.processCode != null and req.processCode != ''">
                    OR i.process_code = #{req.processCode}
                    OR i.operation_code = #{req.processCode}
                  </if>
                  <if test="req.operationCode != null and req.operationCode != ''">
                    OR i.operation_code = #{req.operationCode}
                    OR i.process_code = #{req.operationCode}
                  </if>
                  <if test="req.processName != null and req.processName != ''">
                    OR i.process_name LIKE CONCAT('%', #{req.processName}, '%')
                    OR i.operation_name LIKE CONCAT('%', #{req.processName}, '%')
                  </if>
                  <if test="req.operationName != null and req.operationName != ''">
                    OR i.operation_name LIKE CONCAT('%', #{req.operationName}, '%')
                    OR i.process_name LIKE CONCAT('%', #{req.operationName}, '%')
                  </if>
                </trim>
              </if>
            </script>
            """)
    Long selectUnreadMessageCount(@Param("req") HcProductionInstructionMessagePageReqVO reqVO);

    default PageResult<HcProductionInstructionDO> selectPage(HcProductionInstructionPageReqVO reqVO) {
        return selectPage(reqVO, buildPageQuery(reqVO));
    }

    default List<HcProductionInstructionDO> selectList(HcProductionInstructionPageReqVO reqVO) {
        return selectList(buildPageQuery(reqVO));
    }

    default List<HcProductionInstructionDO> selectOperationInstructionList(
            HcProductionInstructionOperationReqVO reqVO, List<String> statuses) {
        LambdaQueryWrapperX<HcProductionInstructionDO> queryWrapper = new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .inIfPresent(HcProductionInstructionDO::getStatus, statuses)
                .eqIfPresent(HcProductionInstructionDO::getPlanOperationId, reqVO.getPlanOperationId())
                .eqIfPresent(HcProductionInstructionDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(HcProductionInstructionDO::getPlanNo, trimToNull(reqVO.getPlanNo()))
                .eqIfPresent(HcProductionInstructionDO::getBatchNo, trimToNull(reqVO.getBatchNo()))
                .eqIfPresent(HcProductionInstructionDO::getInstructionType, trimToNull(reqVO.getInstructionType()))
                .eqIfPresent(HcProductionInstructionDO::getExecuteStatus, trimToNull(reqVO.getExecuteStatus()));
        String segmentBatchNo = trimToNull(reqVO.getSegmentBatchNo());
        if (StrUtil.isNotBlank(segmentBatchNo)) {
            queryWrapper.and(wrapper -> wrapper.eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                    .or()
                    .isNull(HcProductionInstructionDO::getSegmentBatchNo)
                    .or()
                    .eq(HcProductionInstructionDO::getSegmentBatchNo, ""));
        }

        String processCode = trimToNull(reqVO.getProcessCode());
        String operationCode = trimToNull(reqVO.getOperationCode());
        if (StrUtil.isNotBlank(processCode) || StrUtil.isNotBlank(operationCode)) {
            queryWrapper.and(wrapper -> {
                boolean hasCondition = false;
                if (StrUtil.isNotBlank(processCode)) {
                    wrapper.eq(HcProductionInstructionDO::getProcessCode, processCode)
                            .or()
                            .eq(HcProductionInstructionDO::getOperationCode, processCode);
                    hasCondition = true;
                }
                if (StrUtil.isNotBlank(operationCode)) {
                    if (hasCondition) {
                        wrapper.or();
                    }
                    wrapper.eq(HcProductionInstructionDO::getOperationCode, operationCode)
                            .or()
                            .eq(HcProductionInstructionDO::getProcessCode, operationCode);
                }
            });
        }

        String processName = trimToNull(reqVO.getProcessName());
        String operationName = trimToNull(reqVO.getOperationName());
        if (StrUtil.isNotBlank(processName) || StrUtil.isNotBlank(operationName)) {
            queryWrapper.and(wrapper -> {
                boolean hasCondition = false;
                if (StrUtil.isNotBlank(processName)) {
                    wrapper.like(HcProductionInstructionDO::getProcessName, processName)
                            .or()
                            .like(HcProductionInstructionDO::getOperationName, processName);
                    hasCondition = true;
                }
                if (StrUtil.isNotBlank(operationName)) {
                    if (hasCondition) {
                        wrapper.or();
                    }
                    wrapper.like(HcProductionInstructionDO::getOperationName, operationName)
                            .or()
                            .like(HcProductionInstructionDO::getProcessName, operationName);
                }
            });
        }

        queryWrapper.orderByDesc(HcProductionInstructionDO::getIssuedTime)
                .orderByDesc(HcProductionInstructionDO::getId);
        return selectList(queryWrapper);
    }

    default HcProductionInstructionDO selectLatestInventoryControlInstruction(Long planId, Long planOperationId) {
        if (planId == null || planOperationId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .eq(HcProductionInstructionDO::getPlanId, planId)
                .eq(HcProductionInstructionDO::getPlanOperationId, planOperationId)
                .in(HcProductionInstructionDO::getInstructionType, List.of("FREEZE_STOCK", "UNFREEZE_STOCK"))
                .in(HcProductionInstructionDO::getStatus, List.of("ISSUED", "CONFIRMED"))
                .orderByDesc(HcProductionInstructionDO::getIssuedTime)
                .orderByDesc(HcProductionInstructionDO::getId)
                .last("LIMIT 1"));
    }

    default LambdaQueryWrapperX<HcProductionInstructionDO> buildPageQuery(HcProductionInstructionPageReqVO reqVO) {
        LambdaQueryWrapperX<HcProductionInstructionDO> queryWrapper = new LambdaQueryWrapperX<HcProductionInstructionDO>()
                .likeIfPresent(HcProductionInstructionDO::getPlanNo, trimToNull(reqVO.getPlanNo()))
                .likeIfPresent(HcProductionInstructionDO::getBatchNo, trimToNull(reqVO.getBatchNo()))
                .eqIfPresent(HcProductionInstructionDO::getStatus, trimToNull(reqVO.getStatus()))
                .geIfPresent(HcProductionInstructionDO::getIssuedTime, reqVO.getIssuedTimeStart())
                .leIfPresent(HcProductionInstructionDO::getIssuedTime, reqVO.getIssuedTimeEnd());
        String operationCode = trimToNull(reqVO.getOperationCode());
        if (StrUtil.isNotBlank(operationCode)) {
            queryWrapper.and(wrapper -> wrapper.like(HcProductionInstructionDO::getOperationCode, operationCode)
                    .or()
                    .like(HcProductionInstructionDO::getProcessCode, operationCode));
        }
        String operationName = trimToNull(reqVO.getOperationName());
        if (StrUtil.isNotBlank(operationName)) {
            queryWrapper.and(wrapper -> wrapper.like(HcProductionInstructionDO::getOperationName, operationName)
                    .or()
                    .like(HcProductionInstructionDO::getProcessName, operationName));
        }
        String keyword = trimToNull(reqVO.getKeyword());
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(HcProductionInstructionDO::getInstructionNo, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getPlanNo, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getBatchNo, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getOperationCode, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getOperationName, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getProcessCode, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getProcessName, keyword)
                    .or()
                    .like(HcProductionInstructionDO::getInstructionContent, keyword));
        }
        queryWrapper.orderByDesc(HcProductionInstructionDO::getIssuedTime)
                .orderByDesc(HcProductionInstructionDO::getId);
        return queryWrapper;
    }

    private static String trimToNull(String value) {
        return StrUtil.blankToDefault(value == null ? null : value.trim(), null);
    }

}
