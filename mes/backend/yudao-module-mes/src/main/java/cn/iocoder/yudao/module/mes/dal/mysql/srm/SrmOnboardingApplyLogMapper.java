package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplyLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmOnboardingApplyLogMapper extends BaseMapperX<SrmOnboardingApplyLogDO> {

    default List<SrmOnboardingApplyLogDO> selectListByApplyId(Long applyId) {
        return selectList(new LambdaQueryWrapperX<SrmOnboardingApplyLogDO>()
                .eq(SrmOnboardingApplyLogDO::getApplyId, applyId)
                .orderByAsc(SrmOnboardingApplyLogDO::getCreateTime)
                .orderByAsc(SrmOnboardingApplyLogDO::getId));
    }

}
