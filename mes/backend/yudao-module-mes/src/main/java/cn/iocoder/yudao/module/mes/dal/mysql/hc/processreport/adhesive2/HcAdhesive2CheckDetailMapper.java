package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2CheckDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesive2CheckDetailMapper extends BaseMapperX<HcAdhesive2CheckDetailDO> {

    default List<HcAdhesive2CheckDetailDO> selectListByReportId(Long adhesive2ReportId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2CheckDetailDO>()
                .eq(HcAdhesive2CheckDetailDO::getAdhesive2ReportId, adhesive2ReportId)
                .eq(HcAdhesive2CheckDetailDO::getDeleted, false)
                .orderByAsc(HcAdhesive2CheckDetailDO::getSortNo)
                .orderByAsc(HcAdhesive2CheckDetailDO::getId));
    }

    default void deleteByReportId(Long adhesive2ReportId) {
        delete(new LambdaQueryWrapperX<HcAdhesive2CheckDetailDO>()
                .eq(HcAdhesive2CheckDetailDO::getAdhesive2ReportId, adhesive2ReportId));
    }

    default void deleteByReportIdAndCategory(Long adhesive2ReportId, String itemCategory) {
        delete(new LambdaQueryWrapperX<HcAdhesive2CheckDetailDO>()
                .eq(HcAdhesive2CheckDetailDO::getAdhesive2ReportId, adhesive2ReportId)
                .eq(HcAdhesive2CheckDetailDO::getItemCategory, itemCategory));
    }
}
