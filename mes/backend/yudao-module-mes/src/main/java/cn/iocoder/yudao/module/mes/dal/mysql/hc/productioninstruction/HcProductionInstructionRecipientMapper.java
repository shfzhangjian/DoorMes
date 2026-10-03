package cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionRecipientDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductionInstructionRecipientMapper
        extends BaseMapperX<HcProductionInstructionRecipientDO> {

    default List<HcProductionInstructionRecipientDO> selectListByInstructionId(Long instructionId) {
        return selectList(new LambdaQueryWrapperX<HcProductionInstructionRecipientDO>()
                .eq(HcProductionInstructionRecipientDO::getInstructionId, instructionId)
                .orderByAsc(HcProductionInstructionRecipientDO::getId));
    }

    default HcProductionInstructionRecipientDO selectOneByInstructionIdAndRecipientId(Long instructionId,
                                                                                       Long recipientId) {
        return selectOne(new LambdaQueryWrapperX<HcProductionInstructionRecipientDO>()
                .eq(HcProductionInstructionRecipientDO::getInstructionId, instructionId)
                .eq(HcProductionInstructionRecipientDO::getRecipientId, recipientId));
    }

}
