package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveCheckDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesiveCheckDetailMapper extends BaseMapperX<HcAdhesiveCheckDetailDO> {

    default List<HcAdhesiveCheckDetailDO> selectListByReportId(Long adhesiveReportId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveCheckDetailDO>()
                .eq(HcAdhesiveCheckDetailDO::getAdhesiveReportId, adhesiveReportId)
                .eq(HcAdhesiveCheckDetailDO::getDeleted, false)
                .orderByAsc(HcAdhesiveCheckDetailDO::getSortNo)
                .orderByAsc(HcAdhesiveCheckDetailDO::getId));
    }

    default void deleteByReportId(Long adhesiveReportId) {
        delete(new LambdaQueryWrapperX<HcAdhesiveCheckDetailDO>()
                .eq(HcAdhesiveCheckDetailDO::getAdhesiveReportId, adhesiveReportId));
    }
}
