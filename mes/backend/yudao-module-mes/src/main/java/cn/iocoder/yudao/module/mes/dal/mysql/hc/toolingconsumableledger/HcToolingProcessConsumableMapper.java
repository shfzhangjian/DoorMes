package cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumablePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingProcessConsumableDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcToolingProcessConsumableMapper extends BaseMapperX<HcToolingProcessConsumableDO> {

    default PageResult<HcToolingProcessConsumableDO> selectPage(HcToolingProcessConsumablePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcToolingProcessConsumableDO> selectListByProcessCode(String processCode) {
        LambdaQueryWrapperX<HcToolingProcessConsumableDO> query = new LambdaQueryWrapperX<>();
        query.eq(HcToolingProcessConsumableDO::getStatus, 0)
                .eq(HcToolingProcessConsumableDO::getDeleted, false)
                .orderByAsc(HcToolingProcessConsumableDO::getProcessCode)
                .orderByAsc(HcToolingProcessConsumableDO::getId);
        applyProcessFilter(query, processCode);
        return selectList(query);
    }

    default HcToolingProcessConsumableDO selectOneByProcessAndType(String processCode, String consumableType) {
        return selectOne(new LambdaQueryWrapperX<HcToolingProcessConsumableDO>()
                .eq(HcToolingProcessConsumableDO::getProcessCode, processCode)
                .eq(HcToolingProcessConsumableDO::getConsumableType, consumableType)
                .eq(HcToolingProcessConsumableDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default LambdaQueryWrapperX<HcToolingProcessConsumableDO> buildQuery(HcToolingProcessConsumablePageReqVO reqVO) {
        LambdaQueryWrapperX<HcToolingProcessConsumableDO> query = new LambdaQueryWrapperX<HcToolingProcessConsumableDO>()
                .eqIfPresent(HcToolingProcessConsumableDO::getConsumableType, reqVO.getConsumableType())
                .likeIfPresent(HcToolingProcessConsumableDO::getDefaultErpMaterialCode, reqVO.getDefaultErpMaterialCode())
                .eqIfPresent(HcToolingProcessConsumableDO::getStatus, reqVO.getStatus());
        query.orderByAsc(HcToolingProcessConsumableDO::getProcessCode)
                .orderByAsc(HcToolingProcessConsumableDO::getId);
        applyProcessFilter(query, reqVO.getProcessCode());
        return query;
    }

    private static void applyProcessFilter(LambdaQueryWrapperX<HcToolingProcessConsumableDO> query,
                                           String processCode) {
        String normalized = StrUtil.trimToEmpty(processCode).toUpperCase();
        if ("ADHESIVE".equals(normalized) || "ADHESIVE1".equals(normalized)) {
            query.in(HcToolingProcessConsumableDO::getProcessCode, List.of("ADHESIVE", "ADHESIVE1"));
        } else if (StrUtil.isNotBlank(normalized)) {
            query.eq(HcToolingProcessConsumableDO::getProcessCode, normalized);
        }
    }
}
