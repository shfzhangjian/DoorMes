package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionReviewConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionReviewConfigMapper extends BaseMapperX<QmsExceptionReviewConfigDO> {

    default PageResult<QmsExceptionReviewConfigDO> selectPage(QmsNcReviewConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsExceptionReviewConfigDO>()
                .likeIfPresent(QmsExceptionReviewConfigDO::getUnitName, reqVO.getUnitName())
                .eqIfPresent(QmsExceptionReviewConfigDO::getStatus, reqVO.getStatus())
                .orderByAsc(QmsExceptionReviewConfigDO::getSort)
                .orderByDesc(QmsExceptionReviewConfigDO::getId));
    }

    default List<QmsExceptionReviewConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<QmsExceptionReviewConfigDO>()
                .eq(QmsExceptionReviewConfigDO::getStatus, 0)
                .orderByAsc(QmsExceptionReviewConfigDO::getSort)
                .orderByDesc(QmsExceptionReviewConfigDO::getId));
    }

    default List<QmsExceptionReviewConfigDO> selectEnabledPublicList() {
        return selectList(new LambdaQueryWrapperX<QmsExceptionReviewConfigDO>()
                .eq(QmsExceptionReviewConfigDO::getTenantId, 0L)
                .eq(QmsExceptionReviewConfigDO::getStatus, 0)
                .orderByAsc(QmsExceptionReviewConfigDO::getSort)
                .orderByDesc(QmsExceptionReviewConfigDO::getId));
    }
}
