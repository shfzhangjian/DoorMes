package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcPlanOrderMapper extends BaseMapperX<HcPlanOrderDO> {

    default HcPlanOrderDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcPlanOrderDO>()
                .eq(HcPlanOrderDO::getId, id).last("FOR UPDATE"));
    }

    List<String> CANCELLED_PLAN_STATUSES = List.of("CANCELLED", "CANCELED", "VOID");

    default PageResult<HcPlanOrderDO> selectPage(HcPlanOrderPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcPlanOrderDO> selectList(HcPlanOrderPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    @Select("""
            SELECT plan_no
            FROM mes_pp_plan_order
            WHERE plan_no LIKE CONCAT(#{prefix}, '%')
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<String> selectPlanNosByPrefixIncludeDeleted(@Param("prefix") String prefix);

    @Delete("DELETE FROM mes_pp_plan_order WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Delete("""
            <script>
            DELETE FROM mes_pp_plan_order
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);

    default LambdaQueryWrapperX<HcPlanOrderDO> buildQuery(HcPlanOrderPageReqVO reqVO) {
        List<String> planStatuses = expandPlanStatusAliases(reqVO.getPlanStatuses());
        LambdaQueryWrapperX<HcPlanOrderDO> queryWrapper = new LambdaQueryWrapperX<HcPlanOrderDO>()
                .likeIfPresent(HcPlanOrderDO::getPlanNo, reqVO.getPlanNo())
                .inIfPresent(HcPlanOrderDO::getPlanStatus, planStatuses)
                .eqIfPresent(HcPlanOrderDO::getPlanMode, reqVO.getPlanMode())
                .eqIfPresent(HcPlanOrderDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(HcPlanOrderDO::getMotherMaterialCode, reqVO.getMotherMaterialCode())
                .likeIfPresent(HcPlanOrderDO::getMotherModelCode, reqVO.getMotherModelCode())
                .eqIfPresent(HcPlanOrderDO::getProdType, reqVO.getProdType())
                .eqIfPresent(HcPlanOrderDO::getCategoryCode, reqVO.getCategoryCode())
                .eqIfPresent(HcPlanOrderDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(HcPlanOrderDO::getSizeSpec, reqVO.getSizeSpec())
                .likeIfPresent(HcPlanOrderDO::getRecipeCode, reqVO.getRecipeCode())
                .geIfPresent(HcPlanOrderDO::getPlanDate, reqVO.getPlanDateStart())
                .leIfPresent(HcPlanOrderDO::getPlanDate, reqVO.getPlanDateEnd())
                .geIfPresent(HcPlanOrderDO::getProductionStartDate, reqVO.getProductionStartDateStart())
                .leIfPresent(HcPlanOrderDO::getProductionStartDate, reqVO.getProductionStartDateEnd())
                .geIfPresent(HcPlanOrderDO::getProductionEndDate, reqVO.getProductionEndDateStart())
                .leIfPresent(HcPlanOrderDO::getProductionEndDate, reqVO.getProductionEndDateEnd())
                .geIfPresent(HcPlanOrderDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(HcPlanOrderDO::getCreateTime, reqVO.getCreateTimeEnd());
        if (planStatuses.isEmpty()) {
            queryWrapper.notIn(HcPlanOrderDO::getPlanStatus, CANCELLED_PLAN_STATUSES);
        }
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper.like(HcPlanOrderDO::getPlanNo, keyword)
                    .or()
                    .like(HcPlanOrderDO::getMaterialCode, keyword)
                    .or()
                    .like(HcPlanOrderDO::getMaterialName, keyword)
                    .or()
                    .like(HcPlanOrderDO::getModelCode, keyword)
                    .or()
                    .like(HcPlanOrderDO::getModelName, keyword)
                    .or()
                    .like(HcPlanOrderDO::getSalesOrderNo, keyword)
                    .or()
                    .like(HcPlanOrderDO::getSalesOrderErpNo, keyword)
                    .or()
                    .like(HcPlanOrderDO::getRouteCode, keyword)
                    .or()
                    .like(HcPlanOrderDO::getRouteName, keyword)
                    .or()
                    .like(HcPlanOrderDO::getParentProductionBatchNo, keyword)
                    .or()
                    .like(HcPlanOrderDO::getProductionBatchNo, keyword)
                    .or()
                    .like(HcPlanOrderDO::getInventorySourceBatchNos, keyword)
                    .or()
                    .like(HcPlanOrderDO::getBatchNo, keyword));
        }
        if (StrUtil.isNotBlank(reqVO.getMaterialKeyword())) {
            queryWrapper.and(wrapper -> wrapper.like(HcPlanOrderDO::getMaterialCode, reqVO.getMaterialKeyword())
                    .or()
                    .like(HcPlanOrderDO::getMaterialName, reqVO.getMaterialKeyword()));
        }
        if (StrUtil.isNotBlank(reqVO.getSalesOrderNo())) {
            queryWrapper.and(wrapper -> wrapper.like(HcPlanOrderDO::getSalesOrderNo, reqVO.getSalesOrderNo())
                    .or()
                    .like(HcPlanOrderDO::getSalesOrderErpNo, reqVO.getSalesOrderNo()));
        }
        if (StrUtil.isNotBlank(reqVO.getMotherRollBatchNo())) {
            queryWrapper.and(wrapper -> wrapper.like(HcPlanOrderDO::getParentProductionBatchNo, reqVO.getMotherRollBatchNo())
                    .or()
                    .like(HcPlanOrderDO::getProductionBatchNo, reqVO.getMotherRollBatchNo())
                    .or()
                    .like(HcPlanOrderDO::getInventorySourceBatchNos, reqVO.getMotherRollBatchNo())
                    .or()
                    .like(HcPlanOrderDO::getBatchNo, reqVO.getMotherRollBatchNo()));
        }
        if (StrUtil.isNotBlank(reqVO.getRouteKeyword())) {
            queryWrapper.and(wrapper -> wrapper.like(HcPlanOrderDO::getRouteCode, reqVO.getRouteKeyword())
                    .or()
                    .like(HcPlanOrderDO::getRouteName, reqVO.getRouteKeyword()));
        }
        queryWrapper.orderByDesc(HcPlanOrderDO::getPlanDate)
                .orderByDesc(HcPlanOrderDO::getId);
        return queryWrapper;
    }

    private static List<String> expandPlanStatusAliases(Collection<String> planStatuses) {
        if (planStatuses == null || planStatuses.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        for (String status : planStatuses) {
            if (StrUtil.isBlank(status)) {
                continue;
            }
            String normalized = status.trim().toUpperCase();
            statuses.add(normalized);
            if ("CANCELLED".equals(normalized)) {
                statuses.add("CANCELED");
            } else if ("CANCELED".equals(normalized)) {
                statuses.add("CANCELLED");
            }
        }
        return new ArrayList<>(statuses);
    }

}
