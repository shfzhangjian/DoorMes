package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionEventDO;
import java.util.Arrays;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionEventMapper extends BaseMapperX<QmsExceptionEventDO> {

    Collection<String> TERMINAL_STATUSES = Arrays.asList("CLOSED", "CANCELLED");

    default PageResult<QmsExceptionEventDO> selectPage(QmsExceptionEventPageReqVO reqVO,
                                                       Long currentUserId,
                                                       Collection<Long> participatedExceptionIds,
                                                       Collection<Long> pendingTaskExceptionIds,
                                                       Collection<Long> containmentOverdueExceptionIds) {
        LambdaQueryWrapperX<QmsExceptionEventDO> wrapper = new LambdaQueryWrapperX<QmsExceptionEventDO>()
                .likeIfPresent(QmsExceptionEventDO::getExceptionNo, reqVO.getExceptionNo())
                .eqIfPresent(QmsExceptionEventDO::getExceptionType, reqVO.getExceptionType())
                .eqIfPresent(QmsExceptionEventDO::getExceptionLevel, reqVO.getExceptionLevel())
                .eqIfPresent(QmsExceptionEventDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(QmsExceptionEventDO::getDiscoverTime, reqVO.getDiscoverTime());
        boolean hasMineScope = Boolean.TRUE.equals(reqVO.getPendingMine())
                || Boolean.TRUE.equals(reqVO.getDiscoveredMine())
                || Boolean.TRUE.equals(reqVO.getParticipatedMine());
        if (hasMineScope) {
            Long userId = currentUserId == null ? -1L : currentUserId;
            wrapper.and(scope -> {
                boolean appended = false;
                if (Boolean.TRUE.equals(reqVO.getPendingMine())) {
                    scope.and(pending -> {
                        pending.notIn(QmsExceptionEventDO::getStatus, TERMINAL_STATUSES)
                                .and(owner -> {
                                    owner.eq(QmsExceptionEventDO::getCurrentHandlerUserId, userId);
                                    if (CollUtil.isNotEmpty(pendingTaskExceptionIds)) {
                                        owner.or().in(QmsExceptionEventDO::getId, pendingTaskExceptionIds);
                                    }
                                });
                    });
                    appended = true;
                }
                if (Boolean.TRUE.equals(reqVO.getDiscoveredMine())) {
                    if (appended) {
                        scope.or();
                    }
                    scope.eq(QmsExceptionEventDO::getDiscovererId, userId);
                    appended = true;
                }
                if (Boolean.TRUE.equals(reqVO.getParticipatedMine())) {
                    if (appended) {
                        scope.or();
                    }
                    if (CollUtil.isEmpty(participatedExceptionIds)) {
                        scope.eq(QmsExceptionEventDO::getId, -1L);
                    } else {
                        scope.in(QmsExceptionEventDO::getId, participatedExceptionIds);
                    }
                }
            });
        } else if ("todo".equals(reqVO.getTabType())) {
            wrapper.notIn(QmsExceptionEventDO::getStatus, TERMINAL_STATUSES);
            if (CollUtil.isEmpty(pendingTaskExceptionIds)) {
                wrapper.eq(QmsExceptionEventDO::getCurrentHandlerUserId, currentUserId == null ? -1L : currentUserId);
            } else {
                wrapper.and(item -> item
                        .eq(QmsExceptionEventDO::getCurrentHandlerUserId, currentUserId == null ? -1L : currentUserId)
                        .or()
                        .in(QmsExceptionEventDO::getId, pendingTaskExceptionIds));
            }
        } else if ("initiated".equals(reqVO.getTabType())) {
            wrapper.eq(QmsExceptionEventDO::getDiscovererId, currentUserId == null ? -1L : currentUserId);
        } else if ("processed".equals(reqVO.getTabType())) {
            if (CollUtil.isEmpty(participatedExceptionIds)) {
                wrapper.eq(QmsExceptionEventDO::getId, -1L);
            } else {
                wrapper.in(QmsExceptionEventDO::getId, participatedExceptionIds);
            }
        } else if ("containmentOverdue".equals(reqVO.getTabType())) {
            wrapper.notIn(QmsExceptionEventDO::getStatus, TERMINAL_STATUSES);
            if (CollUtil.isEmpty(containmentOverdueExceptionIds)) {
                wrapper.eq(QmsExceptionEventDO::getId, -1L);
            } else {
                wrapper.in(QmsExceptionEventDO::getId, containmentOverdueExceptionIds);
            }
        }
        wrapper.orderByDesc(QmsExceptionEventDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default QmsExceptionEventDO selectByExceptionNo(String exceptionNo) {
        return selectOne(QmsExceptionEventDO::getExceptionNo, exceptionNo);
    }

    default QmsExceptionEventDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(QmsExceptionEventDO::getProcessInstanceId, processInstanceId);
    }
}
