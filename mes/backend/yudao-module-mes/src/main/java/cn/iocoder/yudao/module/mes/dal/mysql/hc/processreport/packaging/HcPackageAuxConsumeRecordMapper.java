package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackageAuxConsumeRecordDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPackageAuxConsumeRecordMapper extends BaseMapperX<HcPackageAuxConsumeRecordDO> {

    default List<HcPackageAuxConsumeRecordDO> selectListByRecordDate(LocalDate recordDate) {
        return selectList(new LambdaQueryWrapperX<HcPackageAuxConsumeRecordDO>()
                .eq(HcPackageAuxConsumeRecordDO::getRecordDate, recordDate)
                .eq(HcPackageAuxConsumeRecordDO::getDeleted, false)
                .orderByDesc(HcPackageAuxConsumeRecordDO::getRecorderTime)
                .orderByDesc(HcPackageAuxConsumeRecordDO::getId));
    }

    default List<HcPackageAuxConsumeRecordDO> selectListByBiz(String bizType, String bizNo) {
        return selectList(new LambdaQueryWrapperX<HcPackageAuxConsumeRecordDO>()
                .eq(HcPackageAuxConsumeRecordDO::getBizType, bizType)
                .eq(HcPackageAuxConsumeRecordDO::getBizNo, bizNo)
                .eq(HcPackageAuxConsumeRecordDO::getDeleted, false)
                .orderByDesc(HcPackageAuxConsumeRecordDO::getRecorderTime)
                .orderByDesc(HcPackageAuxConsumeRecordDO::getId));
    }

    default boolean existsByBizAndStockId(String bizType, String bizNo, Long stockId) {
        return selectCount(new LambdaQueryWrapperX<HcPackageAuxConsumeRecordDO>()
                .eq(HcPackageAuxConsumeRecordDO::getBizType, bizType)
                .eq(HcPackageAuxConsumeRecordDO::getBizNo, bizNo)
                .eq(HcPackageAuxConsumeRecordDO::getStockId, stockId)
                .eq(HcPackageAuxConsumeRecordDO::getDeleted, false)) > 0;
    }
}
