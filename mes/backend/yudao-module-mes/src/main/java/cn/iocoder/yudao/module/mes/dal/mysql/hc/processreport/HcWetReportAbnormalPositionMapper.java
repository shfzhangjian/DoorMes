package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcWetReportAbnormalPositionDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcWetReportAbnormalPositionMapper extends BaseMapperX<HcWetReportAbnormalPositionDO> {

    default List<HcWetReportAbnormalPositionDO> selectListByOperationReportId(Long operationReportId) {
        return selectList(new LambdaQueryWrapperX<HcWetReportAbnormalPositionDO>()
                .eq(HcWetReportAbnormalPositionDO::getOperationReportId, operationReportId)
                .eq(HcWetReportAbnormalPositionDO::getDeleted, false)
                .orderByAsc(HcWetReportAbnormalPositionDO::getSortOrder)
                .orderByAsc(HcWetReportAbnormalPositionDO::getId));
    }

    default List<HcWetReportAbnormalPositionDO> selectListBySource(String sourceMenuCode, String processStage, Long sourceDetailId) {
        return selectList(new LambdaQueryWrapperX<HcWetReportAbnormalPositionDO>()
                .eq(HcWetReportAbnormalPositionDO::getSourceMenuCode, sourceMenuCode)
                .eq(HcWetReportAbnormalPositionDO::getProcessStage, processStage)
                .eq(HcWetReportAbnormalPositionDO::getSourceDetailId, sourceDetailId)
                .eq(HcWetReportAbnormalPositionDO::getDeleted, false)
                .orderByAsc(HcWetReportAbnormalPositionDO::getSortOrder)
                .orderByAsc(HcWetReportAbnormalPositionDO::getId));
    }

    default void deleteBySource(String sourceMenuCode, String processStage, Long sourceDetailId) {
        delete(new LambdaQueryWrapperX<HcWetReportAbnormalPositionDO>()
                .eq(HcWetReportAbnormalPositionDO::getSourceMenuCode, sourceMenuCode)
                .eq(HcWetReportAbnormalPositionDO::getProcessStage, processStage)
                .eq(HcWetReportAbnormalPositionDO::getSourceDetailId, sourceDetailId));
    }

    @Delete("""
            DELETE FROM mes_sfc_wet_report_abnormal_position
            WHERE source_menu_code = #{sourceMenuCode}
              AND process_stage = #{processStage}
              AND source_detail_id = #{sourceDetailId}
            """)
    int physicalDeleteBySource(@Param("sourceMenuCode") String sourceMenuCode,
                               @Param("processStage") String processStage,
                               @Param("sourceDetailId") Long sourceDetailId);

    @Select("""
            <script>
            SELECT *
            FROM mes_sfc_wet_report_abnormal_position
            WHERE deleted = 0
              AND (
                source_menu_code IS NULL
                OR source_menu_code IN ('WET_REPORT', 'ROUGH_GRINDING_REPORT')
              )
              AND (
                process_stage IS NULL
                OR process_stage IN ('WET', 'FIRST_GRINDING', 'SECOND_GRINDING')
              )
              AND (
                batch_no = #{motherBatchNo}
                OR production_batch_no = #{motherBatchNo}
                OR batch_no LIKE CONCAT(#{motherBatchNo}, '%')
                OR production_batch_no LIKE CONCAT(#{motherBatchNo}, '%')
              )
            ORDER BY
              CASE process_stage
                WHEN 'WET' THEN 1
                WHEN 'FIRST_GRINDING' THEN 2
                WHEN 'SECOND_GRINDING' THEN 3
                ELSE 9
              END,
              COALESCE(sort_order, 0),
              id
            </script>
            """)
    List<HcWetReportAbnormalPositionDO> selectListByRelatedMotherBatchNo(@Param("motherBatchNo") String motherBatchNo);
}
