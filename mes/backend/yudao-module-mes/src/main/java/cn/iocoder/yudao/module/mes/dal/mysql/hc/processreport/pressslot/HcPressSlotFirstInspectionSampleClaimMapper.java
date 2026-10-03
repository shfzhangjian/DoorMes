package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotFirstInspectionSampleClaimDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcPressSlotFirstInspectionSampleClaimMapper
        extends BaseMapperX<HcPressSlotFirstInspectionSampleClaimDO> {

    // 按来源片去重；兼容占用表上线前的首检单，过程加检和异常放行不占用首检样片。
    String OCCUPIED_SOURCE_SQL = """
            SELECT sample_slice.id AS source_slitting_slice_id
            FROM mes_sfc_slitting_slice_record sample_slice
            WHERE sample_slice.deleted = 0
              AND (
                EXISTS (
                  SELECT 1 FROM mes_sfc_press_slot_fai_sample_claim sample_claim
                  WHERE sample_claim.deleted = 0
                    AND sample_claim.tenant_id = sample_slice.tenant_id
                    AND sample_claim.source_slitting_slice_id = sample_slice.id
                )
                OR EXISTS (
                  SELECT 1 FROM mes_qms_fai_order sample_fai
                  WHERE sample_fai.deleted = 0
                    AND sample_fai.tenant_id = sample_slice.tenant_id
                    AND sample_fai.product_batch_no = sample_slice.slice_serial_no
                    AND sample_fai.source_module = 'PRESS_SLOT_REPORT'
                    AND sample_fai.process_category = 'PRESS_SLOT'
                    AND COALESCE(sample_fai.status, '') != 'CANCELED'
                    AND COALESCE(sample_fai.source_report_no, '') NOT LIKE '%-PROCESS-CHECK-%'
                    AND COALESCE(sample_fai.source_report_no, '') NOT LIKE '%-ABNORMAL-RELEASE-%'
                )
              )
            """;

    @Select(OCCUPIED_SOURCE_SQL + " AND sample_slice.plan_id = #{planId}")
    List<Long> selectOccupiedSourceIds(@Param("planId") Long planId);

    default List<HcPressSlotFirstInspectionSampleClaimDO> selectListByFirstFaiId(Long firstFaiId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotFirstInspectionSampleClaimDO>()
                .eq(HcPressSlotFirstInspectionSampleClaimDO::getFirstFaiId, firstFaiId));
    }

}
