package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplySignDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmOnboardingApplySignMapper extends BaseMapperX<SrmOnboardingApplySignDO> {

    default List<SrmOnboardingApplySignDO> selectListByApplyId(Long applyId) {
        return selectList(new LambdaQueryWrapperX<SrmOnboardingApplySignDO>()
                .eq(SrmOnboardingApplySignDO::getApplyId, applyId)
                .orderByAsc(SrmOnboardingApplySignDO::getDeptName)
                .orderByAsc(SrmOnboardingApplySignDO::getUserName)
                .orderByAsc(SrmOnboardingApplySignDO::getId));
    }

    default SrmOnboardingApplySignDO selectByApplyIdAndUserId(Long applyId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmOnboardingApplySignDO>()
                .eq(SrmOnboardingApplySignDO::getApplyId, applyId)
                .eq(SrmOnboardingApplySignDO::getUserId, userId)
                .eq(SrmOnboardingApplySignDO::getSignStatus, "PENDING")
                .last("LIMIT 1"));
    }

    default List<SrmOnboardingApplySignDO> selectPendingListByApplyIdAndUserId(Long applyId, Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmOnboardingApplySignDO>()
                .eq(SrmOnboardingApplySignDO::getApplyId, applyId)
                .eq(SrmOnboardingApplySignDO::getUserId, userId)
                .eq(SrmOnboardingApplySignDO::getSignStatus, "PENDING")
                .orderByAsc(SrmOnboardingApplySignDO::getId));
    }

    default void deleteByApplyId(Long applyId) {
        delete(SrmOnboardingApplySignDO::getApplyId, applyId);
    }

}
