package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgManualPieceDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcNgManualPieceMapper extends BaseMapperX<HcNgManualPieceDO> {

    default HcNgManualPieceDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcNgManualPieceDO>()
                .eq(HcNgManualPieceDO::getId, id)
                .eq(HcNgManualPieceDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default boolean existsActiveByPieceNo(String pieceNo) {
        if (pieceNo == null || pieceNo.isBlank()) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<HcNgManualPieceDO>()
                .eq(HcNgManualPieceDO::getPieceNo, pieceNo)
                .notIn(HcNgManualPieceDO::getRecordStatus, List.of("VOID", "UNFROZEN_CLOSED"))
                .eq(HcNgManualPieceDO::getDeleted, false)) > 0;
    }

    default boolean existsActiveByPieceNoExcludeId(String pieceNo, Long excludeId) {
        if (pieceNo == null || pieceNo.isBlank()) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<HcNgManualPieceDO>()
                .eq(HcNgManualPieceDO::getPieceNo, pieceNo)
                .neIfPresent(HcNgManualPieceDO::getId, excludeId)
                .notIn(HcNgManualPieceDO::getRecordStatus, List.of("VOID", "UNFROZEN_CLOSED"))
                .eq(HcNgManualPieceDO::getDeleted, false)) > 0;
    }
}
