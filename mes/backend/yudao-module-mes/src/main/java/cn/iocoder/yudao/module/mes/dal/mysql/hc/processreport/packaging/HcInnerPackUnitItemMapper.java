package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcInnerPackUnitItemMapper extends BaseMapperX<HcInnerPackUnitItemDO> {

    default List<HcInnerPackUnitItemDO> selectListByInnerUnitId(Long innerUnitId) {
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getInnerUnitId, innerUnitId)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByAsc(HcInnerPackUnitItemDO::getId));
    }

    /**
     * 按包装单 ID 批量查询片号明细，避免分类视图逐包查询造成 N+1 SQL。
     */
    default List<HcInnerPackUnitItemDO> selectListByInnerUnitIds(Collection<Long> innerUnitIds) {
        if (innerUnitIds == null || innerUnitIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .in(HcInnerPackUnitItemDO::getInnerUnitId, innerUnitIds)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByAsc(HcInnerPackUnitItemDO::getId));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit_item
            WHERE inner_unit_id = #{innerUnitId}
              AND deleted = 0
            ORDER BY id ASC
            FOR UPDATE
            """)
    List<HcInnerPackUnitItemDO> selectListByInnerUnitIdForUpdate(@Param("innerUnitId") Long innerUnitId);

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit_item
            WHERE id = #{id}
              AND deleted = 0
            FOR UPDATE
            """)
    HcInnerPackUnitItemDO selectByIdForUpdate(@Param("id") Long id);

    default List<HcInnerPackUnitItemDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getPlanOperationId, planOperationId)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByAsc(HcInnerPackUnitItemDO::getId));
    }

    default HcInnerPackUnitItemDO selectBySliceBatchNo(String sliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getSliceBatchNo, sliceBatchNo)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitItemDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit_item
            WHERE slice_batch_no = #{sliceBatchNo}
              AND deleted = 0
            ORDER BY id DESC
            FOR UPDATE
            """)
    List<HcInnerPackUnitItemDO> selectListBySliceBatchNoForUpdate(@Param("sliceBatchNo") String sliceBatchNo);

    default HcInnerPackUnitItemDO selectByInnerUnitNoAndSliceBatchNo(String innerUnitNo, String sliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getInnerUnitNo, innerUnitNo)
                .and(wrapper -> wrapper.eq(HcInnerPackUnitItemDO::getSliceBatchNo, sliceBatchNo)
                        .or()
                        .eq(HcInnerPackUnitItemDO::getProductionBatchNo, sliceBatchNo))
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcInnerPackUnitItemDO selectByCutRoundReportId(Long sourceCutRoundReportId) {
        return selectOne(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getSourceCutRoundReportId, sourceCutRoundReportId)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcInnerPackUnitItemDO selectByManualPieceId(Long sourceManualPieceId) {
        return selectOne(new LambdaQueryWrapperX<HcInnerPackUnitItemDO>()
                .eq(HcInnerPackUnitItemDO::getSourceManualPieceId, sourceManualPieceId)
                .eq(HcInnerPackUnitItemDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitItemDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("""
            <script>
            DELETE FROM mes_sfc_inner_pack_unit_item
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int deletePhysicallyByIds(@Param("ids") Collection<Long> ids);
}
