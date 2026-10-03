package cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintProductItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcVisualPrintProductItemMapper extends BaseMapperX<HcVisualPrintProductItemDO> {

    default List<HcVisualPrintProductItemDO> selectListByCustomerInfoId(Long customerInfoId) {
        return selectList(new LambdaQueryWrapperX<HcVisualPrintProductItemDO>()
                .eq(HcVisualPrintProductItemDO::getCustomerInfoId, customerInfoId)
                .orderByAsc(HcVisualPrintProductItemDO::getSourceRow)
                .orderByAsc(HcVisualPrintProductItemDO::getId));
    }

    default List<HcVisualPrintProductItemDO> selectListByCustomerInfoIds(List<Long> customerInfoIds) {
        if (customerInfoIds == null || customerInfoIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcVisualPrintProductItemDO>()
                .in(HcVisualPrintProductItemDO::getCustomerInfoId, customerInfoIds)
                .orderByAsc(HcVisualPrintProductItemDO::getSourceRow)
                .orderByAsc(HcVisualPrintProductItemDO::getId));
    }

    default HcVisualPrintProductItemDO selectByCustomerInfoIdAndSourceRow(Long tenantId, Long customerInfoId,
            Integer sourceRow) {
        return selectOne(new LambdaQueryWrapperX<HcVisualPrintProductItemDO>()
                .eq(HcVisualPrintProductItemDO::getTenantId, tenantId)
                .eq(HcVisualPrintProductItemDO::getCustomerInfoId, customerInfoId)
                .eq(HcVisualPrintProductItemDO::getSourceRow, sourceRow));
    }

    default HcVisualPrintProductItemDO selectByIdAndCustomerInfoId(Long id, Long customerInfoId) {
        return selectOne(new LambdaQueryWrapperX<HcVisualPrintProductItemDO>()
                .eq(HcVisualPrintProductItemDO::getId, id)
                .eq(HcVisualPrintProductItemDO::getCustomerInfoId, customerInfoId));
    }

    default void physicalDeleteByCustomerInfoId(Long customerInfoId) {
        delete(new LambdaQueryWrapperX<HcVisualPrintProductItemDO>()
                .eq(HcVisualPrintProductItemDO::getCustomerInfoId, customerInfoId));
    }

}
