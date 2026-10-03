package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcOuterPackBoxDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcOuterPackBoxMapper extends BaseMapperX<HcOuterPackBoxDO> {

    default List<HcOuterPackBoxDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcOuterPackBoxDO>()
                .eq(HcOuterPackBoxDO::getPlanOperationId, planOperationId)
                .eq(HcOuterPackBoxDO::getDeleted, false)
                .orderByDesc(HcOuterPackBoxDO::getId));
    }

    default HcOuterPackBoxDO selectByBoxNo(String outerBoxNo) {
        return selectOne(new LambdaQueryWrapperX<HcOuterPackBoxDO>()
                .eq(HcOuterPackBoxDO::getOuterBoxNo, outerBoxNo)
                .eq(HcOuterPackBoxDO::getDeleted, false)
                .orderByDesc(HcOuterPackBoxDO::getId)
                .last("LIMIT 1"));
    }
}
