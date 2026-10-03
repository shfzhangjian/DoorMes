package cn.iocoder.yudao.module.mes.dal.mysql.hc.paramrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcParamRecordDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcParamRecordMapper extends BaseMapperX<HcParamRecordDO> {

    default PageResult<HcParamRecordDO> selectPage(HcParamRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcParamRecordDO>()
                .eqIfPresent(HcParamRecordDO::getReportId, reqVO.getReportId())
                .likeIfPresent(HcParamRecordDO::getReportNo, reqVO.getReportNo())
                .likeIfPresent(HcParamRecordDO::getParamCode, reqVO.getParamCode())
                .likeIfPresent(HcParamRecordDO::getParamName, reqVO.getParamName())
                .eqIfPresent(HcParamRecordDO::getJudgeResult, reqVO.getJudgeResult())
                .orderByDesc(HcParamRecordDO::getId));
    }

    default List<HcParamRecordDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<HcParamRecordDO>()
                .eq(HcParamRecordDO::getReportId, reportId)
                .orderByAsc(HcParamRecordDO::getId));
    }

    default void deleteByReportId(Long reportId) {
        delete(new LambdaQueryWrapperX<HcParamRecordDO>().eq(HcParamRecordDO::getReportId, reportId));
    }
}
