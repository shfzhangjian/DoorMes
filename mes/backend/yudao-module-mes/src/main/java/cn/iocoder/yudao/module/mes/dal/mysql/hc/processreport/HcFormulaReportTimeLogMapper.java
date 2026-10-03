package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcFormulaReportTimeLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFormulaReportTimeLogMapper extends BaseMapperX<HcFormulaReportTimeLogDO> {

    default List<HcFormulaReportTimeLogDO> selectListByOperationReportId(Long operationReportId) {
        return selectList(new LambdaQueryWrapperX<HcFormulaReportTimeLogDO>()
                .eq(HcFormulaReportTimeLogDO::getOperationReportId, operationReportId)
                .orderByDesc(HcFormulaReportTimeLogDO::getChangeTime)
                .orderByDesc(HcFormulaReportTimeLogDO::getId));
    }

}
