package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSparePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcCutRoundSpareMapper extends BaseMapperX<HcCutRoundSpareDO> {

    default PageResult<HcCutRoundSpareDO> selectPage(HcCutRoundSparePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcCutRoundSpareDO> selectList(HcCutRoundSparePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcCutRoundSpareDO> buildQuery(HcCutRoundSparePageReqVO reqVO) {
        LambdaQueryWrapperX<HcCutRoundSpareDO> wrapper = new LambdaQueryWrapperX<HcCutRoundSpareDO>()
                .likeIfPresent(HcCutRoundSpareDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcCutRoundSpareDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcCutRoundSpareDO::getSpareType, reqVO.getSpareType())
                .likeIfPresent(HcCutRoundSpareDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcCutRoundSpareDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcCutRoundSpareDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcCutRoundSpareDO::getStatus, reqVO.getStatus());
        wrapper.eq(HcCutRoundSpareDO::getDeleted, false)
                .orderByDesc(HcCutRoundSpareDO::getWarningFlag)
                .orderByAsc(HcCutRoundSpareDO::getEquipmentCode)
                .orderByAsc(HcCutRoundSpareDO::getSpareType)
                .orderByDesc(HcCutRoundSpareDO::getId);
        return wrapper;
    }

    default HcCutRoundSpareDO selectOneByEquipmentAndType(Long equipmentId, String spareType) {
        return selectOne(new LambdaQueryWrapperX<HcCutRoundSpareDO>()
                .eq(HcCutRoundSpareDO::getEquipmentId, equipmentId)
                .eq(HcCutRoundSpareDO::getSpareType, spareType)
                .eq(HcCutRoundSpareDO::getDeleted, false)
                .orderByDesc(HcCutRoundSpareDO::getId)
                .last("LIMIT 1"));
    }

    default HcCutRoundSpareDO selectForUpdate(Long equipmentId, String spareType) {
        return selectOne(new LambdaQueryWrapperX<HcCutRoundSpareDO>()
                .eq(HcCutRoundSpareDO::getEquipmentId, equipmentId)
                .eq(HcCutRoundSpareDO::getSpareType, spareType)
                .orderByDesc(HcCutRoundSpareDO::getId).last("LIMIT 1 FOR UPDATE"));
    }

    default List<HcCutRoundSpareDO> lockAllStates() {
        return selectList(new LambdaQueryWrapperX<HcCutRoundSpareDO>()
                .orderByAsc(HcCutRoundSpareDO::getId).last("FOR UPDATE"));
    }

    @Delete("DELETE FROM mes_md_cut_round_spare WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
