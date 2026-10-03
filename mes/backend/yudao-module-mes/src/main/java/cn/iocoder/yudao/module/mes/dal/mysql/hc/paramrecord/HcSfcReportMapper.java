package cn.iocoder.yudao.module.mes.dal.mysql.hc.paramrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcSfcReportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcSfcReportMapper extends BaseMapperX<HcSfcReportDO> {

    default HcSfcReportDO selectByReportNo(String reportNo) {
        return selectOne(new LambdaQueryWrapperX<HcSfcReportDO>().eq(HcSfcReportDO::getReportNo, reportNo));
    }
}
