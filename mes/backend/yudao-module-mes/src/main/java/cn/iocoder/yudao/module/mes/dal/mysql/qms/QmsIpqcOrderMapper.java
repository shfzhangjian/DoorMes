package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIpqcOrderMapper extends BaseMapperX<QmsIpqcOrderDO> {

    default PageResult<QmsIpqcOrderDO> selectPage(QmsIpqcPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsIpqcOrderDO>()
                .likeIfPresent(QmsIpqcOrderDO::getIpqcNo, reqVO.getIpqcNo())
                .likeIfPresent(QmsIpqcOrderDO::getMachineCode, reqVO.getMachineCode())
                .likeIfPresent(QmsIpqcOrderDO::getWorkOrderNo, reqVO.getWorkOrderNo())
                .likeIfPresent(QmsIpqcOrderDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(QmsIpqcOrderDO::getInspectionType, reqVO.getInspectionType())
                .eqIfPresent(QmsIpqcOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsIpqcOrderDO::getJudgment, reqVO.getJudgment())
                .betweenIfPresent(QmsIpqcOrderDO::getInspectionTime, reqVO.getInspectionTime())
                .orderByDesc(QmsIpqcOrderDO::getId));
    }

    default QmsIpqcOrderDO selectByIpqcNo(String ipqcNo, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsIpqcOrderDO>()
                .eq(QmsIpqcOrderDO::getIpqcNo, ipqcNo)
                .neIfPresent(QmsIpqcOrderDO::getId, excludeId));
    }

    default List<QmsIpqcOrderDO> selectListByStatuses(Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIpqcOrderDO>()
                .in(QmsIpqcOrderDO::getStatus, statuses)
                .orderByAsc(QmsIpqcOrderDO::getScheduledTime)
                .orderByAsc(QmsIpqcOrderDO::getId));
    }
}
