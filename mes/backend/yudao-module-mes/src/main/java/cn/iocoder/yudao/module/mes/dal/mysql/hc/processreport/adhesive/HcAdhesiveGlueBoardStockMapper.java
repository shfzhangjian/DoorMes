package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcAdhesiveGlueBoardStockMapper extends BaseMapperX<HcAdhesiveGlueBoardStockDO> {

    String ACCESSORY_CATEGORY_GLUE_BOARD = "GLUE_BOARD";

    default PageResult<HcAdhesiveGlueBoardStockDO> selectPage(HcAdhesiveGlueBoardStockPageReqVO reqVO) {
        return selectPage(reqVO, (Collection<Long>) null);
    }

    default PageResult<HcAdhesiveGlueBoardStockDO> selectPage(HcAdhesiveGlueBoardStockPageReqVO reqVO,
                                                              Collection<Long> excludeStockIds) {
        return selectPage(reqVO, excludeStockIds, null);
    }

    default PageResult<HcAdhesiveGlueBoardStockDO> selectPage(HcAdhesiveGlueBoardStockPageReqVO reqVO,
                                                              Collection<Long> excludeStockIds,
                                                              Collection<String> excludeBatchNos) {
        return selectPage(reqVO, buildQuery(reqVO, excludeStockIds, excludeBatchNos));
    }

    default List<HcAdhesiveGlueBoardStockDO> selectList(HcAdhesiveGlueBoardStockPageReqVO reqVO) {
        return selectList(buildQuery(reqVO, null, null));
    }

    default LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO> buildQuery(HcAdhesiveGlueBoardStockPageReqVO reqVO,
                                                                       Collection<Long> excludeStockIds,
                                                                       Collection<String> excludeBatchNos) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, reqVO.getAccessoryCategory())
                .likeIfPresent(HcAdhesiveGlueBoardStockDO::getGlueBoardMaterialCode, reqVO.getGlueBoardMaterialCode())
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getGlueBoardModel, reqVO.getGlueBoardModel())
                .likeIfPresent(HcAdhesiveGlueBoardStockDO::getGlueBoardBatchNo, reqVO.getGlueBoardBatchNo())
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getStockStatus, reqVO.getStockStatus())
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getQualityStatus, reqVO.getQualityStatus())
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getErpTransferStatus, reqVO.getErpTransferStatus())
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getReceiveDate, reqVO.getReceiveDate())
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getReceiveDate)
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId);
        List<String> candidateGlueBoardModels = splitCandidateValues(reqVO.getCandidateGlueBoardModels());
        if (!candidateGlueBoardModels.isEmpty()) {
            wrapper.in(HcAdhesiveGlueBoardStockDO::getGlueBoardModel, candidateGlueBoardModels);
        }
        if (StrUtil.isNotBlank(reqVO.getSourceProcessCode())) {
            wrapper.apply("COALESCE(CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.ledgerBalanceSettled')) ELSE NULL END, 'false') <> 'true'");
            wrapper.apply("(CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END IS NULL "
                                    + "OR CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END <> {0} "
                                    + "OR NOT EXISTS (SELECT 1 FROM mes_md_tooling_consumable_ledger l "
                                    + "WHERE l.id = CAST(CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.toolingLedgerId')) ELSE NULL END AS UNSIGNED) "
                                    + "AND l.deleted = b'0' AND l.usage_status IN ({1}, {2})))",
                            "TOOLING_CONSUMABLE_LEDGER", "USED_UP", "RETURNED");
            String sourceProcessCode = StrUtil.trim(reqVO.getSourceProcessCode()).toUpperCase();
            if ("ADHESIVE".equals(sourceProcessCode) || "ADHESIVE1".equals(sourceProcessCode)) {
                wrapper.apply("(CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END IS NULL "
                                + "OR CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END <> {0} "
                                + "OR CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.processCode')) ELSE NULL END IN ({1}, {2}))",
                        "TOOLING_CONSUMABLE_LEDGER", "ADHESIVE", "ADHESIVE1");
            } else {
                wrapper.apply("(CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END IS NULL "
                                + "OR CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END <> {0} "
                                + "OR CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.processCode')) ELSE NULL END = {1})",
                        "TOOLING_CONSUMABLE_LEDGER", sourceProcessCode);
            }
        }
        if (excludeStockIds != null && !excludeStockIds.isEmpty()) {
            wrapper.notIn(HcAdhesiveGlueBoardStockDO::getId, excludeStockIds);
        }
        if (excludeBatchNos != null && !excludeBatchNos.isEmpty()) {
            wrapper.notIn(HcAdhesiveGlueBoardStockDO::getGlueBoardBatchNo, excludeBatchNos);
        }
        return wrapper;
    }

    private static List<String> splitCandidateValues(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    default HcAdhesiveGlueBoardStockDO selectByBatchNo(String glueBoardBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eq(HcAdhesiveGlueBoardStockDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eq(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, ACCESSORY_CATEGORY_GLUE_BOARD)
                .eq(HcAdhesiveGlueBoardStockDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveGlueBoardStockDO selectByBatchNoAndModel(String glueBoardBatchNo, String glueBoardModel) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eq(HcAdhesiveGlueBoardStockDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eq(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, ACCESSORY_CATEGORY_GLUE_BOARD)
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getGlueBoardModel, glueBoardModel)
                .eq(HcAdhesiveGlueBoardStockDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveGlueBoardStockDO selectByToolingLedgerId(Long toolingLedgerId) {
        if (toolingLedgerId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eq(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, ACCESSORY_CATEGORY_GLUE_BOARD)
                .eq(HcAdhesiveGlueBoardStockDO::getDeleted, false)
                .and(link -> link
                        .eq(HcAdhesiveGlueBoardStockDO::getToolingLedgerId, toolingLedgerId)
                        .or(legacy -> legacy
                                .apply("CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.sourceType')) ELSE NULL END = {0}",
                                        "TOOLING_CONSUMABLE_LEDGER")
                                .apply("CASE WHEN JSON_VALID(extra_json) THEN JSON_UNQUOTE(JSON_EXTRACT(extra_json, '$.toolingLedgerId')) ELSE NULL END = {0}",
                                        String.valueOf(toolingLedgerId))))
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId)
                .last("LIMIT 1"));
    }

    @Select("SELECT * FROM mes_sfc_adhesive_glue_board_stock "
            + "WHERE id = #{id} AND deleted = b'0' FOR UPDATE")
    HcAdhesiveGlueBoardStockDO selectByIdForUpdate(@Param("id") Long id);

    default HcAdhesiveGlueBoardStockDO selectByLatestInspectionId(Long latestInspectionId) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eq(HcAdhesiveGlueBoardStockDO::getLatestInspectionId, latestInspectionId)
                .eq(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, ACCESSORY_CATEGORY_GLUE_BOARD)
                .eq(HcAdhesiveGlueBoardStockDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveGlueBoardStockDO selectByBatchNoAndCategory(String glueBoardBatchNo, String accessoryCategory) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardStockDO>()
                .eq(HcAdhesiveGlueBoardStockDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eqIfPresent(HcAdhesiveGlueBoardStockDO::getAccessoryCategory, accessoryCategory)
                .eq(HcAdhesiveGlueBoardStockDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardStockDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_sfc_adhesive_glue_board_stock "
            + "WHERE tenant_id = #{tenantId} AND accessory_category = #{accessoryCategory}")
    int physicalDeleteByTenantIdAndCategory(@Param("tenantId") Long tenantId,
                                            @Param("accessoryCategory") String accessoryCategory);
}
