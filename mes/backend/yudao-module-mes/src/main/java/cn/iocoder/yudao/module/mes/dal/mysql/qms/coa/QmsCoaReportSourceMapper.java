package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportSourceDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaReportSourceMapper extends BaseMapperX<QmsCoaReportSourceDO> {
    default List<QmsCoaReportSourceDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapper<QmsCoaReportSourceDO>()
                .eq(QmsCoaReportSourceDO::getReportId, reportId)
                .orderByAsc(QmsCoaReportSourceDO::getProcessCode)
                .orderByDesc(QmsCoaReportSourceDO::getQaTime));
    }

    @Delete("DELETE FROM mes_qms_coa_report_source WHERE report_id = #{reportId}")
    int deleteByReportId(Long reportId);
}
