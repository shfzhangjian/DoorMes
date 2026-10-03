package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingOuterCheckDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgShippingOuterCheckMapper extends BaseMapperX<HcFgShippingOuterCheckDO> {

    default HcFgShippingOuterCheckDO selectByFormRecordId(Long formRecordId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingOuterCheckDO>()
                .eq(HcFgShippingOuterCheckDO::getFormRecordId, formRecordId)
                .eq(HcFgShippingOuterCheckDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcFgShippingOuterCheckDO> selectActiveListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingOuterCheckDO>()
                .eq(HcFgShippingOuterCheckDO::getNoticeId, noticeId)
                .eq(HcFgShippingOuterCheckDO::getDeleted, false)
                .orderByDesc(HcFgShippingOuterCheckDO::getId));
    }
}
