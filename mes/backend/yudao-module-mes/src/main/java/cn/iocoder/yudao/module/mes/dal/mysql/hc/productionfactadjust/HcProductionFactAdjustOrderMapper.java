package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust.HcProductionFactAdjustOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductionFactAdjustOrderMapper extends BaseMapperX<HcProductionFactAdjustOrderDO> {

    default PageResult<HcProductionFactAdjustOrderDO> selectPage(PageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcProductionFactAdjustOrderDO>()
                .eqIfPresent(HcProductionFactAdjustOrderDO::getStatus, reqVO.getStatus())
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank(), wrapper -> wrapper
                        .like(HcProductionFactAdjustOrderDO::getAdjustNo, reqVO.getKeyword())
                        .or().like(HcProductionFactAdjustOrderDO::getPlanNo, reqVO.getKeyword())
                        .or().like(HcProductionFactAdjustOrderDO::getSegmentBatchNo, reqVO.getKeyword())
                        .or().like(HcProductionFactAdjustOrderDO::getInstructionNo, reqVO.getKeyword()))
                .orderByDesc(HcProductionFactAdjustOrderDO::getId));
    }
}
