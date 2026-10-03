package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcLabelPrintLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcLabelPrintLogMapper extends BaseMapperX<HcLabelPrintLogDO> {

    default List<HcLabelPrintLogDO> selectListByBizNo(String labelType, String bizNo) {
        return selectList(new LambdaQueryWrapperX<HcLabelPrintLogDO>()
                .eq(HcLabelPrintLogDO::getLabelType, labelType)
                .eq(HcLabelPrintLogDO::getBizNo, bizNo)
                .eq(HcLabelPrintLogDO::getDeleted, false)
                .orderByDesc(HcLabelPrintLogDO::getId));
    }
}
