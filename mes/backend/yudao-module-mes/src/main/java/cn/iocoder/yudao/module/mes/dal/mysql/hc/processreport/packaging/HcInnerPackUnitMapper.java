package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcInnerPackUnitMapper extends BaseMapperX<HcInnerPackUnitDO> {

    default List<HcInnerPackUnitDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitDO>()
                .eq(HcInnerPackUnitDO::getPlanOperationId, planOperationId)
                .eq(HcInnerPackUnitDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitDO::getId));
    }

    default List<HcInnerPackUnitDO> selectListByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitDO>()
                .eq(HcInnerPackUnitDO::getPlanOperationId, planOperationId)
                .eq(HcInnerPackUnitDO::getBatchNo, batchNo)
                .eq(HcInnerPackUnitDO::getDeleted, false)
                .orderByAsc(HcInnerPackUnitDO::getId));
    }

    default HcInnerPackUnitDO selectByUnitNo(String innerUnitNo) {
        return selectOne(new LambdaQueryWrapperX<HcInnerPackUnitDO>()
                .eq(HcInnerPackUnitDO::getInnerUnitNo, innerUnitNo)
                .eq(HcInnerPackUnitDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit
            WHERE inner_unit_no = #{innerUnitNo}
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
            """)
    HcInnerPackUnitDO selectByUnitNoForUpdate(@Param("innerUnitNo") String innerUnitNo);

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit
            WHERE id = #{id}
            FOR UPDATE
            """)
    HcInnerPackUnitDO selectByIdForUpdate(@Param("id") Long id);

    default List<HcInnerPackUnitDO> selectStockListByLocationCode(String locationCode) {
        return selectList(new LambdaQueryWrapperX<HcInnerPackUnitDO>()
                .eq(HcInnerPackUnitDO::getLocationCode, locationCode)
                .in(HcInnerPackUnitDO::getUnitStatus, List.of("INBOUND_LOCKED", "INBOUNDED"))
                .eq(HcInnerPackUnitDO::getDeleted, false)
                .orderByDesc(HcInnerPackUnitDO::getInboundTime)
                .orderByDesc(HcInnerPackUnitDO::getLockTime)
                .orderByDesc(HcInnerPackUnitDO::getId));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit
            WHERE deleted = 0
              AND unit_status IN ('PACKED', 'INBOUND_LOCKED', 'INBOUNDED')
              AND (location_code IS NOT NULL OR unit_status IN ('PACKED', 'INBOUND_LOCKED'))
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                OR batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR location_code LIKE CONCAT('%', #{keyword}, '%')
                OR location_name LIKE CONCAT('%', #{keyword}, '%')
                OR material_code LIKE CONCAT('%', #{keyword}, '%')
                OR model_code LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY lock_time DESC, create_time DESC, id DESC
            LIMIT 200
            """)
    List<HcInnerPackUnitDO> selectFgInboundPackageList(@Param("keyword") String keyword);

    @Select("""
            SELECT COUNT(1)
            FROM mes_sfc_inner_pack_unit
            WHERE deleted = 0
              AND (
                (#{shelfStatus} = 'PENDING'
                  AND unit_status IN ('PACKED', 'INBOUND_LOCKED')
                  AND (location_code IS NULL OR location_code = ''))
                OR (#{shelfStatus} = 'SHELVED'
                  AND unit_status = 'INBOUNDED'
                  AND location_code IS NOT NULL)
              )
              AND (#{qualityStatus} IS NULL OR #{qualityStatus} = ''
                OR (#{qualityStatus} = 'NG' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG'))
                OR (#{qualityStatus} = 'OK' AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status IN ('NG', 'FROZEN')))
                OR (#{qualityStatus} = 'FROZEN' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'FROZEN') AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG')))
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                OR batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR location_code LIKE CONCAT('%', #{keyword}, '%')
                OR location_name LIKE CONCAT('%', #{keyword}, '%')
                OR material_code LIKE CONCAT('%', #{keyword}, '%')
                OR model_code LIKE CONCAT('%', #{keyword}, '%'))
            """)
    Long selectFgInboundPackagePageCount(@Param("keyword") String keyword,
                                         @Param("shelfStatus") String shelfStatus,
                                         @Param("qualityStatus") String qualityStatus);

    @Select("""
            SELECT *
            FROM mes_sfc_inner_pack_unit
            WHERE deleted = 0
              AND (
                (#{shelfStatus} = 'PENDING'
                  AND unit_status IN ('PACKED', 'INBOUND_LOCKED')
                  AND (location_code IS NULL OR location_code = ''))
                OR (#{shelfStatus} = 'SHELVED'
                  AND unit_status = 'INBOUNDED'
                  AND location_code IS NOT NULL)
              )
              AND (#{qualityStatus} IS NULL OR #{qualityStatus} = ''
                OR (#{qualityStatus} = 'NG' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG'))
                OR (#{qualityStatus} = 'OK' AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status IN ('NG', 'FROZEN')))
                OR (#{qualityStatus} = 'FROZEN' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'FROZEN') AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = mes_sfc_inner_pack_unit.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG')))
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                OR batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR location_code LIKE CONCAT('%', #{keyword}, '%')
                OR location_name LIKE CONCAT('%', #{keyword}, '%')
                OR material_code LIKE CONCAT('%', #{keyword}, '%')
                OR model_code LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY lock_time DESC, create_time DESC, id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<HcInnerPackUnitDO> selectFgInboundPackagePage(@Param("keyword") String keyword,
                                                        @Param("shelfStatus") String shelfStatus,
                                                        @Param("qualityStatus") String qualityStatus,
                                                        @Param("offset") Integer offset,
                                                        @Param("limit") Integer limit);

    @Select("""
            SELECT DISTINCT u.*
            FROM mes_sfc_inner_pack_unit u
            LEFT JOIN mes_sfc_inner_pack_unit_item i
              ON i.inner_unit_id = u.id
             AND i.deleted = 0
            WHERE u.deleted = 0
              AND (
                (#{shelfStatus} = 'PENDING'
                  AND u.unit_status IN ('PACKED', 'INBOUND_LOCKED')
                  AND (u.location_code IS NULL OR u.location_code = ''))
                OR (#{shelfStatus} = 'SHELVED'
                  AND u.unit_status = 'INBOUNDED'
                  AND u.location_code IS NOT NULL)
              )
              AND (#{qualityStatus} IS NULL OR #{qualityStatus} = ''
                OR (#{qualityStatus} = 'NG' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = u.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG'))
                OR (#{qualityStatus} = 'OK' AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = u.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status IN ('NG', 'FROZEN')))
                OR (#{qualityStatus} = 'FROZEN' AND EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = u.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'FROZEN') AND NOT EXISTS (
                  SELECT 1 FROM mes_sfc_inner_pack_unit_item quality_item
                  WHERE quality_item.inner_unit_id = u.id
                    AND quality_item.deleted = 0
                    AND quality_item.quality_status = 'NG')))
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR u.inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                OR u.batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR u.location_code LIKE CONCAT('%', #{keyword}, '%')
                OR u.location_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.material_code LIKE CONCAT('%', #{keyword}, '%')
                OR u.model_code LIKE CONCAT('%', #{keyword}, '%')
                OR i.slice_batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR i.production_batch_no LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY u.lock_time DESC, u.create_time DESC, u.id DESC
            """)
    List<HcInnerPackUnitDO> selectFgInboundPackageSegmentList(@Param("keyword") String keyword,
                                                               @Param("shelfStatus") String shelfStatus,
                                                               @Param("qualityStatus") String qualityStatus);

    @Select("""
            SELECT DISTINCT u.*
            FROM mes_sfc_inner_pack_unit u
            LEFT JOIN mes_sfc_inner_pack_unit_item i
              ON i.inner_unit_id = u.id
             AND i.deleted = 0
            WHERE u.deleted = 0
              AND u.unit_status IN ('PACKED', 'INBOUND_LOCKED')
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR u.inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                OR u.batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR u.material_code LIKE CONCAT('%', #{keyword}, '%')
                OR u.model_code LIKE CONCAT('%', #{keyword}, '%')
                OR i.slice_batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR i.production_batch_no LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY u.lock_time DESC, u.create_time DESC, u.id DESC
            """)
    List<HcInnerPackUnitDO> selectFgInboundPackedPackageSegmentList(@Param("keyword") String keyword);

    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(inner_unit_no, LENGTH(#{prefix}) + 1) AS UNSIGNED)), 0)
            FROM mes_sfc_inner_pack_unit
            WHERE inner_unit_no LIKE CONCAT(#{prefix}, '%')
            """)
    Integer selectMaxPackageNoSerial(@Param("prefix") String prefix);

    @Select("SELECT GET_LOCK(#{lockName}, #{timeoutSeconds})")
    Integer tryAcquireInboundPackageNoLock(@Param("lockName") String lockName,
                                           @Param("timeoutSeconds") Integer timeoutSeconds);

    @Select("SELECT RELEASE_LOCK(#{lockName})")
    Integer releaseInboundPackageNoLock(@Param("lockName") String lockName);

    @Delete("""
            DELETE FROM mes_sfc_inner_pack_unit
            WHERE id = #{id}
            """)
    int deletePhysicallyById(@Param("id") Long id);
}
