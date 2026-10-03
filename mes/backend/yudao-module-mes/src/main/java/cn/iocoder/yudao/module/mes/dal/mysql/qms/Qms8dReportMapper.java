package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dReportDO;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Qms8dReportMapper extends BaseMapperX<Qms8dReportDO> {

    default PageResult<Qms8dReportDO> selectPage(Qms8dReportPageReqVO reqVO,
                                                 Long currentUserId,
                                                 Collection<Long> participatedReportIds) {
        LambdaQueryWrapperX<Qms8dReportDO> wrapper = new LambdaQueryWrapperX<Qms8dReportDO>()
                .likeIfPresent(Qms8dReportDO::getReportNo, reqVO.getReportNo())
                .likeIfPresent(Qms8dReportDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(Qms8dReportDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(Qms8dReportDO::getCurrentStep, reqVO.getCurrentStep())
                .eqIfPresent(Qms8dReportDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(Qms8dReportDO::getIssueDate, reqVO.getIssueDate())
                .betweenIfPresent(Qms8dReportDO::getTargetDate, reqVO.getTargetDate());
        if ("todo".equals(reqVO.getTabType())) {
            wrapper.eq(Qms8dReportDO::getCurrentHandlerUserId, currentUserId == null ? -1L : currentUserId)
                    .ne(Qms8dReportDO::getStatus, "PASSED");
        } else if ("initiated".equals(reqVO.getTabType())) {
            wrapper.eq(Qms8dReportDO::getInitiatorUserId, currentUserId == null ? -1L : currentUserId);
        } else if ("processed".equals(reqVO.getTabType())) {
            if (CollUtil.isEmpty(participatedReportIds)) {
                wrapper.eq(Qms8dReportDO::getId, -1L);
            } else {
                wrapper.in(Qms8dReportDO::getId, participatedReportIds);
            }
        }
        wrapper.orderByDesc(Qms8dReportDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default Qms8dReportDO selectByReportNo(String reportNo) {
        return selectOne(new LambdaQueryWrapperX<Qms8dReportDO>()
                .eq(Qms8dReportDO::getReportNo, reportNo));
    }

    default Qms8dReportDO selectBySource(String sourceType, Long sourceId, String sourceNo) {
        LambdaQueryWrapperX<Qms8dReportDO> wrapper = new LambdaQueryWrapperX<Qms8dReportDO>()
                .eq(Qms8dReportDO::getSourceType, sourceType)
                .eqIfPresent(Qms8dReportDO::getSourceId, sourceId)
                .eqIfPresent(Qms8dReportDO::getSourceNo, sourceNo)
                .orderByDesc(Qms8dReportDO::getId);
        return CollUtil.getFirst(selectList(wrapper));
    }
}
