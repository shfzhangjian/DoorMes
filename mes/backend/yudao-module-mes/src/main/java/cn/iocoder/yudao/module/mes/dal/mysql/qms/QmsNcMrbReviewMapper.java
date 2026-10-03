package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcMrbReviewDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcMrbReviewMapper extends BaseMapperX<QmsNcMrbReviewDO> {

    default List<QmsNcMrbReviewDO> selectListByNcRecordId(Long ncRecordId) {
        return selectList(new LambdaQueryWrapperX<QmsNcMrbReviewDO>()
                .eq(QmsNcMrbReviewDO::getNcRecordId, ncRecordId)
                .orderByAsc(QmsNcMrbReviewDO::getSort)
                .orderByAsc(QmsNcMrbReviewDO::getId));
    }

    default List<QmsNcMrbReviewDO> selectListByHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<QmsNcMrbReviewDO>()
                .and(wrapper -> wrapper.eq(QmsNcMrbReviewDO::getHandlerUserId, handlerUserId)
                        .or().eq(QmsNcMrbReviewDO::getDelegateUserId, handlerUserId)
                        .or().eq(QmsNcMrbReviewDO::getActualHandlerUserId, handlerUserId)));
    }

    default List<QmsNcMrbReviewDO> selectPendingListByHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<QmsNcMrbReviewDO>()
                .and(wrapper -> wrapper.eq(QmsNcMrbReviewDO::getHandlerUserId, handlerUserId)
                        .or().eq(QmsNcMrbReviewDO::getDelegateUserId, handlerUserId))
                .eq(QmsNcMrbReviewDO::getReviewStatus, "PENDING"));
    }

    default List<QmsNcMrbReviewDO> selectHandledListByActualHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<QmsNcMrbReviewDO>()
                .eq(QmsNcMrbReviewDO::getActualHandlerUserId, handlerUserId)
                .eq(QmsNcMrbReviewDO::getReviewStatus, "HANDLED"));
    }
}
