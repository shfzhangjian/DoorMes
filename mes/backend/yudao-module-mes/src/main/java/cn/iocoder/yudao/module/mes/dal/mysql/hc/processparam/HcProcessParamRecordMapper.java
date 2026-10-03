package cn.iocoder.yudao.module.mes.dal.mysql.hc.processparam;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam.HcProcessParamRecordDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProcessParamRecordMapper extends BaseMapperX<HcProcessParamRecordDO> {

    default HcProcessParamRecordDO selectOneByUnique(Long tenantId,
                                                     String batchNo,
                                                     String processCode,
                                                     String paramCode) {
        return selectOne(new LambdaQueryWrapperX<HcProcessParamRecordDO>()
                .eq(HcProcessParamRecordDO::getTenantId, tenantId)
                .eq(HcProcessParamRecordDO::getBatchNo, batchNo)
                .eq(HcProcessParamRecordDO::getProcessCode, processCode)
                .eq(HcProcessParamRecordDO::getParamCode, paramCode)
                .eq(HcProcessParamRecordDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcProcessParamRecordDO> selectListByBatchNosAndProcessCodes(Collection<String> batchNos,
                                                                             Collection<String> processCodes,
                                                                             String paramCode) {
        if (batchNos == null || batchNos.isEmpty() || processCodes == null || processCodes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcProcessParamRecordDO>()
                .in(HcProcessParamRecordDO::getBatchNo, batchNos)
                .in(HcProcessParamRecordDO::getProcessCode, processCodes)
                .eqIfPresent(HcProcessParamRecordDO::getParamCode, paramCode)
                .eq(HcProcessParamRecordDO::getDeleted, false)
                .orderByDesc(HcProcessParamRecordDO::getRecordTime)
                .orderByDesc(HcProcessParamRecordDO::getId));
    }

}
