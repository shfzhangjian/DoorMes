package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableRuleDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcEquipmentConsumableRuleMapper extends BaseMapperX<HcEquipmentConsumableRuleDO> {

    default List<HcEquipmentConsumableRuleDO> selectEnabledList(String processCode, String consumableType) {
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableRuleDO>()
                .eq(HcEquipmentConsumableRuleDO::getProcessCode, processCode)
                .eq(HcEquipmentConsumableRuleDO::getConsumableType, consumableType)
                .eq(HcEquipmentConsumableRuleDO::getStatus, 0)
                .eq(HcEquipmentConsumableRuleDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableRuleDO::getEquipmentId)
                .orderByDesc(HcEquipmentConsumableRuleDO::getWorkCenterId)
                .orderByDesc(HcEquipmentConsumableRuleDO::getId));
    }
}
