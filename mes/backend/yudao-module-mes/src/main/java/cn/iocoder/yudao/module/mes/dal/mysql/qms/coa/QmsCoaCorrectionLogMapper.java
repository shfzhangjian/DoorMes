package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaCorrectionLogDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaCorrectionLogMapper extends BaseMapperX<QmsCoaCorrectionLogDO> {
    default List<QmsCoaCorrectionLogDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapper<QmsCoaCorrectionLogDO>()
                .eq(QmsCoaCorrectionLogDO::getReportId, reportId)
                .orderByDesc(QmsCoaCorrectionLogDO::getCorrectionTime)
                .orderByDesc(QmsCoaCorrectionLogDO::getId));
    }
}
