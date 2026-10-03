package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundCheckDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcCutRoundCheckDetailMapper extends BaseMapperX<HcCutRoundCheckDetailDO> {

    default List<HcCutRoundCheckDetailDO> selectListByReportId(Long cutRoundReportId) {
        return selectList(new LambdaQueryWrapperX<HcCutRoundCheckDetailDO>()
                .eq(HcCutRoundCheckDetailDO::getCutRoundReportId, cutRoundReportId)
                .eq(HcCutRoundCheckDetailDO::getDeleted, false)
                .orderByAsc(HcCutRoundCheckDetailDO::getSortNo)
                .orderByAsc(HcCutRoundCheckDetailDO::getId));
    }

    default void deleteByReportId(Long cutRoundReportId) {
        delete(new LambdaQueryWrapperX<HcCutRoundCheckDetailDO>()
                .eq(HcCutRoundCheckDetailDO::getCutRoundReportId, cutRoundReportId));
    }
}
