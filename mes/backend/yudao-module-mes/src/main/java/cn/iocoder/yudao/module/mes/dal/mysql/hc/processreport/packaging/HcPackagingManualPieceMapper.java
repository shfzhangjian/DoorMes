package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcPackagingManualPieceMapper extends BaseMapperX<HcPackagingManualPieceDO> {

    default HcPackagingManualPieceDO selectBySliceBatchNo(String sliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPackagingManualPieceDO>()
                .eq(HcPackagingManualPieceDO::getSliceBatchNo, sliceBatchNo)
                .eq(HcPackagingManualPieceDO::getDeleted, false)
                .orderByDesc(HcPackagingManualPieceDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_packaging_manual_piece
            WHERE slice_batch_no = #{sliceBatchNo}
              AND deleted = 0
            ORDER BY id DESC
            FOR UPDATE
            """)
    List<HcPackagingManualPieceDO> selectListBySliceBatchNoForUpdate(@Param("sliceBatchNo") String sliceBatchNo);

    default HcPackagingManualPieceDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcPackagingManualPieceDO>()
                .eq(HcPackagingManualPieceDO::getId, id)
                .eq(HcPackagingManualPieceDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default List<HcPackagingManualPieceDO> selectWaitPackagingList(String keyword) {
        LambdaQueryWrapperX<HcPackagingManualPieceDO> wrapper = new LambdaQueryWrapperX<HcPackagingManualPieceDO>()
                .eq(HcPackagingManualPieceDO::getRecordStatus, "WAIT_PACKAGING")
                .eq(HcPackagingManualPieceDO::getDeleted, false);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like(HcPackagingManualPieceDO::getSliceBatchNo, keyword)
                    .or().like(HcPackagingManualPieceDO::getSegmentBatchNo, keyword)
                    .or().like(HcPackagingManualPieceDO::getMaterialCode, keyword)
                    .or().like(HcPackagingManualPieceDO::getMaterialName, keyword)
                    .or().like(HcPackagingManualPieceDO::getModelCode, keyword));
        }
        return selectList(wrapper
                .orderByDesc(HcPackagingManualPieceDO::getProductionDate)
                .orderByDesc(HcPackagingManualPieceDO::getId));
    }

    @Update("""
            UPDATE mes_sfc_packaging_manual_piece
            SET record_status = 'WAIT_PACKAGING',
                inner_unit_id = NULL,
                inner_unit_no = NULL
            WHERE id = #{id}
              AND deleted = 0
            """)
    int resetPackagingStatus(@Param("id") Long id);

    @Update("""
            UPDATE mes_sfc_packaging_manual_piece
            SET record_status = 'VOID',
                delete_reason = #{deleteReason},
                delete_user_name = #{deleteUserName},
                delete_time = #{deleteTime},
                updater = #{updater},
                update_time = #{deleteTime},
                deleted = 1
            WHERE id = #{id}
              AND record_status = 'WAIT_PACKAGING'
              AND deleted = 0
            """)
    int voidWaitPackagingPiece(@Param("id") Long id,
                               @Param("deleteReason") String deleteReason,
                               @Param("deleteUserName") String deleteUserName,
                               @Param("deleteTime") LocalDateTime deleteTime,
                               @Param("updater") String updater);
}
