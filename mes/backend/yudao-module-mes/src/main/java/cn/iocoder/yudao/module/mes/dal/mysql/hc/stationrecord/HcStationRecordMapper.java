package cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

@Mapper
public interface HcStationRecordMapper extends BaseMapperX<HcStationRecordDO> {

    default PageResult<HcStationRecordDO> selectPage(HcStationRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcStationRecordDO> queryWrapper = new LambdaQueryWrapperX<HcStationRecordDO>()
                .likeIfPresent(HcStationRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcStationRecordDO::getOperationName, reqVO.getOperationName())
                .likeIfPresent(HcStationRecordDO::getFormName, reqVO.getFormName())
                .eqIfPresent(HcStationRecordDO::getFormCode, reqVO.getFormCode())
                .neIfPresent(HcStationRecordDO::getFormCode, reqVO.getExcludeFormCode())
                .likeIfPresent(HcStationRecordDO::getEquipmentName, reqVO.getEquipmentName())
                .likeIfPresent(HcStationRecordDO::getEquipmentCode, reqVO.getEquipmentCode())
                .eqIfPresent(HcStationRecordDO::getRecordScope, reqVO.getRecordScope())
                .likeIfPresent(HcStationRecordDO::getConfirmUserName, reqVO.getConfirmUserName())
                .geIfPresent(HcStationRecordDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(HcStationRecordDO::getCreateTime, reqVO.getCreateTimeEnd())
                .geIfPresent(HcStationRecordDO::getConfirmTime, reqVO.getConfirmTimeStart())
                .leIfPresent(HcStationRecordDO::getConfirmTime, reqVO.getConfirmTimeEnd());
        if (StringUtils.hasText(reqVO.getFormCodePrefix())) {
            queryWrapper.likeRight(HcStationRecordDO::getFormCode, reqVO.getFormCodePrefix().trim());
        }
        if (StringUtils.hasText(reqVO.getCreateUserName())) {
            String createUserName = reqVO.getCreateUserName().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(HcStationRecordDO::getRecordUserName, createUserName)
                    .or().like(HcStationRecordDO::getCreator, createUserName));
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(HcStationRecordDO::getPlanNo, keyword)
                    .or().like(HcStationRecordDO::getOperationName, keyword)
                    .or().like(HcStationRecordDO::getFormName, keyword)
                    .or().like(HcStationRecordDO::getFormCode, keyword)
                    .or().like(HcStationRecordDO::getEquipmentName, keyword)
                    .or().like(HcStationRecordDO::getEquipmentCode, keyword));
        }
        queryWrapper.orderByDesc(HcStationRecordDO::getCreateTime)
                .orderByDesc(HcStationRecordDO::getId);
        return selectPage(reqVO, queryWrapper);
    }

    default List<HcStationRecordDO> selectByPlanOperationAndFormIds(Long planOperationId, Collection<Long> formIds) {
        if (planOperationId == null || formIds == null || formIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getPlanOperationId, planOperationId)
                .in(HcStationRecordDO::getFormId, formIds)
                .orderByAsc(HcStationRecordDO::getFormId)
                .orderByDesc(HcStationRecordDO::getId));
    }

    default HcStationRecordDO selectOneByPlanOperationAndFormCode(Long planOperationId, String formCode) {
        return selectOne(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getPlanOperationId, planOperationId)
                .eq(HcStationRecordDO::getFormCode, formCode)
                .orderByDesc(HcStationRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcStationRecordDO selectLegacyPlanRecordForDailyFallback(Long planOperationId, String formCode) {
        if (planOperationId == null || formCode == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getPlanOperationId, planOperationId)
                .eq(HcStationRecordDO::getFormCode, formCode)
                .and(wrapper -> wrapper.isNull(HcStationRecordDO::getRecordScope)
                        .or().eq(HcStationRecordDO::getRecordScope, "")
                        .or().eq(HcStationRecordDO::getRecordScope, "PLAN_OPERATION"))
                .eq(HcStationRecordDO::getDeleted, false)
                .orderByDesc(HcStationRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcStationRecordDO selectOneByEquipmentDaily(Long equipmentId, LocalDate recordDate, String formCode) {
        if (equipmentId == null || recordDate == null || formCode == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getRecordScope, "EQUIPMENT_DAILY")
                .eq(HcStationRecordDO::getEquipmentId, equipmentId)
                .eq(HcStationRecordDO::getRecordDate, recordDate)
                .eq(HcStationRecordDO::getFormCode, formCode)
                .eq(HcStationRecordDO::getDeleted, false)
                .orderByDesc(HcStationRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcStationRecordDO> selectListByEquipmentDaily(Long equipmentId, LocalDate recordDate, Collection<String> formCodes) {
        if (equipmentId == null || recordDate == null || formCodes == null || formCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getRecordScope, "EQUIPMENT_DAILY")
                .eq(HcStationRecordDO::getEquipmentId, equipmentId)
                .eq(HcStationRecordDO::getRecordDate, recordDate)
                .in(HcStationRecordDO::getFormCode, formCodes)
                .eq(HcStationRecordDO::getDeleted, false)
                .orderByAsc(HcStationRecordDO::getFormCode)
                .orderByDesc(HcStationRecordDO::getId));
    }

    default HcStationRecordDO selectOneByBiz(String recordScope, String bizType, Long bizId, String formCode) {
        if (recordScope == null || bizType == null || bizId == null || formCode == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getRecordScope, recordScope)
                .eq(HcStationRecordDO::getBizType, bizType)
                .eq(HcStationRecordDO::getBizId, bizId)
                .eq(HcStationRecordDO::getFormCode, formCode)
                .eq(HcStationRecordDO::getDeleted, false)
                .orderByDesc(HcStationRecordDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_sfc_station_record WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
