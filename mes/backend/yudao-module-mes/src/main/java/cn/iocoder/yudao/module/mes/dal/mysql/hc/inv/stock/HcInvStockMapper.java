package cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcInvStockMapper extends BaseMapperX<HcInvStockDO> {

    default PageResult<HcInvStockDO> selectPage(HcInvStockPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcInvStockDO> selectList(HcInvStockPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default HcInvStockDO selectOneBySource(String sourceType, String sourceTable, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getSourceType, sourceType)
                .eq(HcInvStockDO::getSourceTable, sourceTable)
                .eq(HcInvStockDO::getSourceId, sourceId)
                .eq(HcInvStockDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcInvStockDO selectOneBySourceForUpdate(String sourceType, String sourceTable, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getSourceType, sourceType)
                .eq(HcInvStockDO::getSourceTable, sourceTable)
                .eq(HcInvStockDO::getSourceId, sourceId)
                .eq(HcInvStockDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default HcInvStockDO selectOneByStockKey(String warehouseCode, Long materialId, String batchNo, String locationCode) {
        return selectOne(new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getWarehouseCode, warehouseCode)
                .eq(HcInvStockDO::getMaterialId, materialId)
                .eq(HcInvStockDO::getBatchNo, batchNo)
                .eq(HcInvStockDO::getLocationCode, locationCode)
                .eq(HcInvStockDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcInvStockDO selectOneByStockKeyForUpdate(String warehouseCode, Long materialId, String batchNo,
                                                        String locationCode) {
        return selectOne(new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getWarehouseCode, warehouseCode)
                .eq(HcInvStockDO::getMaterialId, materialId)
                .eq(HcInvStockDO::getBatchNo, batchNo)
                .eq(HcInvStockDO::getLocationCode, locationCode)
                .eq(HcInvStockDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default HcInvStockDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getId, id)
                .eq(HcInvStockDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default LambdaQueryWrapper<HcInvStockDO> buildQuery(HcInvStockPageReqVO reqVO) {
        LambdaQueryWrapper<HcInvStockDO> query = new LambdaQueryWrapperX<HcInvStockDO>()
                .eqIfPresent(HcInvStockDO::getWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(HcInvStockDO::getStockType, reqVO.getStockType())
                .eqIfPresent(HcInvStockDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(HcInvStockDO::getSourcePlanNo, reqVO.getSourcePlanNo())
                .likeIfPresent(HcInvStockDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .likeIfPresent(HcInvStockDO::getSourceParentBatchNo, reqVO.getSourceParentBatchNo())
                .eqIfPresent(HcInvStockDO::getRecipeCode, reqVO.getRecipeCode())
                .eqIfPresent(HcInvStockDO::getSpecSize, reqVO.getSpecSize())
                .eqIfPresent(HcInvStockDO::getOpSeq, reqVO.getOpSeq())
                .eqIfPresent(HcInvStockDO::getSegmentCode, reqVO.getSegmentCode())
                .likeIfPresent(HcInvStockDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcInvStockDO::getModelNo, reqVO.getModelNo())
                .likeIfPresent(HcInvStockDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcInvStockDO::getQualityStatus, reqVO.getQualityStatus())
                .eqIfPresent(HcInvStockDO::getBizStatus, reqVO.getBizStatus())
                .geIfPresent(HcInvStockDO::getProductionDate, reqVO.getProductionDateStart())
                .leIfPresent(HcInvStockDO::getProductionDate, reqVO.getProductionDateEnd())
                .orderByDesc(HcInvStockDO::getUpdateTime);
        if (reqVO.getExcludeSourceTypes() != null && !reqVO.getExcludeSourceTypes().isEmpty()) {
            query.and(wrapper -> wrapper.isNull(HcInvStockDO::getSourceType)
                    .or()
                    .notIn(HcInvStockDO::getSourceType, reqVO.getExcludeSourceTypes()));
        }
        if (reqVO.getExcludeOpCodes() != null && !reqVO.getExcludeOpCodes().isEmpty()) {
            query.and(wrapper -> wrapper.isNull(HcInvStockDO::getOpCode)
                    .or()
                    .notIn(HcInvStockDO::getOpCode, reqVO.getExcludeOpCodes()));
        }
        if (reqVO.getExcludeOpNameKeywords() != null && !reqVO.getExcludeOpNameKeywords().isEmpty()) {
            reqVO.getExcludeOpNameKeywords().forEach(keyword -> {
                if (keyword != null && !keyword.isBlank()) {
                    query.and(wrapper -> wrapper.isNull(HcInvStockDO::getOpName)
                            .or()
                            .notLike(HcInvStockDO::getOpName, keyword));
                }
            });
        }
        if (Boolean.TRUE.equals(reqVO.getIncludeUnavailable())) {
            query.gt(HcInvStockDO::getOnHandQty, 0);
        } else {
            query.gt(HcInvStockDO::getAvailableQty, 0);
        }
        if (Boolean.TRUE.equals(reqVO.getOnlyShareable())) {
            query.gt(HcInvStockDO::getShareableQty, 0);
        }
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper
                    .like(HcInvStockDO::getBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getMaterialCode, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getMaterialName, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getModelNo, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getSourcePlanNo, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getSourceBatchNo, reqVO.getKeyword())
                    .or()
                    .like(HcInvStockDO::getSourceParentBatchNo, reqVO.getKeyword()));
        }
        return query;
    }

}
