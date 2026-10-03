package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcReviewConfigDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcReviewConfigMapper extends BaseMapperX<QmsNcReviewConfigDO> {

    String FINAL_APPROVER_UNIT_CODE = "FINAL_APPROVER";

    default PageResult<QmsNcReviewConfigDO> selectPage(QmsNcReviewConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsNcReviewConfigDO>()
                .likeIfPresent(QmsNcReviewConfigDO::getUnitName, reqVO.getUnitName())
                .eqIfPresent(QmsNcReviewConfigDO::getStatus, reqVO.getStatus())
                .orderByAsc(QmsNcReviewConfigDO::getSort)
                .orderByDesc(QmsNcReviewConfigDO::getId));
    }

    default List<QmsNcReviewConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<QmsNcReviewConfigDO>()
                .eq(QmsNcReviewConfigDO::getStatus, 0)
                .orderByAsc(QmsNcReviewConfigDO::getSort)
                .orderByDesc(QmsNcReviewConfigDO::getId));
    }

    default QmsNcReviewConfigDO selectEnabledFinalApprover() {
        return selectOne(new LambdaQueryWrapperX<QmsNcReviewConfigDO>()
                .eq(QmsNcReviewConfigDO::getUnitCode, FINAL_APPROVER_UNIT_CODE)
                .eq(QmsNcReviewConfigDO::getStatus, 0));
    }

    default List<QmsNcReviewConfigDO> selectEnabledListByUnitNames(Collection<String> unitNames) {
        return selectList(new LambdaQueryWrapperX<QmsNcReviewConfigDO>()
                .inIfPresent(QmsNcReviewConfigDO::getUnitName, unitNames)
                .eq(QmsNcReviewConfigDO::getStatus, 0)
                .orderByAsc(QmsNcReviewConfigDO::getSort)
                .orderByDesc(QmsNcReviewConfigDO::getId));
    }
}
