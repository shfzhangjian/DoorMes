package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaReportItemMapper extends BaseMapperX<QmsCoaReportItemDO> {
    default List<QmsCoaReportItemDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapper<QmsCoaReportItemDO>()
                .eq(QmsCoaReportItemDO::getReportId, reportId)
                .orderByAsc(QmsCoaReportItemDO::getSortNo)
                .orderByAsc(QmsCoaReportItemDO::getId));
    }

    @Delete("DELETE FROM mes_qms_coa_report_item WHERE report_id = #{reportId}")
    int deleteByReportId(Long reportId);
}
