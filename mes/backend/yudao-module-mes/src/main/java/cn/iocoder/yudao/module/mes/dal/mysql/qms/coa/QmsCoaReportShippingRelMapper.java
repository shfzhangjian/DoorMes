package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportShippingRelDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaReportShippingRelMapper extends BaseMapperX<QmsCoaReportShippingRelDO> {
    default List<QmsCoaReportShippingRelDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapper<QmsCoaReportShippingRelDO>()
                .eq(QmsCoaReportShippingRelDO::getReportId, reportId)
                .orderByAsc(QmsCoaReportShippingRelDO::getShippingNoticeItemId));
    }

    @Delete("DELETE FROM mes_qms_coa_report_shipping_rel WHERE report_id = #{reportId}")
    int deleteByReportId(Long reportId);
}
