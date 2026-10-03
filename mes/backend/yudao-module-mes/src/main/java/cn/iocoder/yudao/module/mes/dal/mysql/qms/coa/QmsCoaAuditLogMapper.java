package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaAuditLogDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaAuditLogMapper extends BaseMapperX<QmsCoaAuditLogDO> {
    default List<QmsCoaAuditLogDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapper<QmsCoaAuditLogDO>()
                .eq(QmsCoaAuditLogDO::getReportId, reportId)
                .orderByDesc(QmsCoaAuditLogDO::getActionTime)
                .orderByDesc(QmsCoaAuditLogDO::getId));
    }
}
