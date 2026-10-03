package cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionChangeoverPieceDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductionInstructionChangeoverPieceMapper
        extends BaseMapperX<HcProductionInstructionChangeoverPieceDO> {

    default Long selectCountByInstructionId(Long instructionId) {
        return selectCount(new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                .eq(HcProductionInstructionChangeoverPieceDO::getInstructionId, instructionId));
    }

    default HcProductionInstructionChangeoverPieceDO selectOneByInstructionIdAndPieceNo(
            Long instructionId, String pieceNo) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                .eq(HcProductionInstructionChangeoverPieceDO::getInstructionId, instructionId)
                .eq(HcProductionInstructionChangeoverPieceDO::getPieceNo, pieceNo));
    }

    default HcProductionInstructionChangeoverPieceDO selectLatestByPlanOperationAndPieceNo(
            Long planId, Long planOperationId, String pieceNo) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                .eq(HcProductionInstructionChangeoverPieceDO::getPlanId, planId)
                .eq(HcProductionInstructionChangeoverPieceDO::getPlanOperationId, planOperationId)
                .eq(HcProductionInstructionChangeoverPieceDO::getPieceNo, pieceNo)
                .eq(HcProductionInstructionChangeoverPieceDO::getStatus, "DONE")
                .orderByDesc(HcProductionInstructionChangeoverPieceDO::getScanTime)
                .orderByDesc(HcProductionInstructionChangeoverPieceDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcProductionInstructionChangeoverPieceDO> selectListByInstructionId(Long instructionId) {
        return selectList(new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                .eq(HcProductionInstructionChangeoverPieceDO::getInstructionId, instructionId)
                .orderByAsc(HcProductionInstructionChangeoverPieceDO::getScanTime)
                .orderByAsc(HcProductionInstructionChangeoverPieceDO::getId));
    }

}
