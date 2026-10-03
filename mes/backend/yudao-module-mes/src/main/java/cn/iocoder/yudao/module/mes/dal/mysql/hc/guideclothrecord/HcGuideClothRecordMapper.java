package cn.iocoder.yudao.module.mes.dal.mysql.hc.guideclothrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcGuideClothRecordMapper extends BaseMapperX<HcGuideClothRecordDO> {

    default PageResult<HcGuideClothRecordDO> selectPage(HcGuideClothRecordPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcGuideClothRecordDO> selectList(HcGuideClothRecordPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default HcGuideClothRecordDO selectCurrentByLineCode(String lineCode) {
        return selectOne(new LambdaQueryWrapperX<HcGuideClothRecordDO>()
                .eq(HcGuideClothRecordDO::getLineCode, lineCode)
                .eq(HcGuideClothRecordDO::getCurrentFlag, 0)
                .orderByDesc(HcGuideClothRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcGuideClothRecordDO selectCurrentByLineCodes(Collection<String> lineCodes) {
        List<String> cleanLineCodes = lineCodes == null ? List.of() : lineCodes.stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .toList();
        if (cleanLineCodes.isEmpty()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcGuideClothRecordDO>()
                .in(HcGuideClothRecordDO::getLineCode, cleanLineCodes)
                .eq(HcGuideClothRecordDO::getCurrentFlag, 0)
                .orderByDesc(HcGuideClothRecordDO::getId)
                .last("LIMIT 1"));
    }

    default LambdaQueryWrapperX<HcGuideClothRecordDO> buildQuery(HcGuideClothRecordPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcGuideClothRecordDO>()
                .likeIfPresent(HcGuideClothRecordDO::getLineName, reqVO.getLineName())
                .likeIfPresent(HcGuideClothRecordDO::getLineCode, reqVO.getLineCode())
                .likeIfPresent(HcGuideClothRecordDO::getReplacePlanNo, reqVO.getReplacePlanNo())
                .likeIfPresent(HcGuideClothRecordDO::getPetBatchNo, reqVO.getPetBatchNo())
                .likeIfPresent(HcGuideClothRecordDO::getGuideClothBatchNo, reqVO.getGuideClothBatchNo())
                .likeIfPresent(HcGuideClothRecordDO::getPetModel, reqVO.getPetModel())
                .eqIfPresent(HcGuideClothRecordDO::getCurrentFlag, reqVO.getCurrentFlag())
                .orderByDesc(HcGuideClothRecordDO::getCurrentFlag)
                .orderByDesc(HcGuideClothRecordDO::getReplaceTime)
                .orderByDesc(HcGuideClothRecordDO::getId);
    }

    @Delete("DELETE FROM mes_md_guide_cloth_record WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
