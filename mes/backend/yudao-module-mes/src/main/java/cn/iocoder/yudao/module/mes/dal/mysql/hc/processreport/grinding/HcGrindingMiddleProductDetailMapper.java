package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingMiddleProductDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcGrindingMiddleProductDetailMapper extends BaseMapperX<HcGrindingMiddleProductDetailDO> {

    default List<HcGrindingMiddleProductDetailDO> selectListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingMiddleProductDetailDO>()
                .eq(HcGrindingMiddleProductDetailDO::getRecordId, recordId)
                .eq(HcGrindingMiddleProductDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingMiddleProductDetailDO::getSeq)
                .orderByAsc(HcGrindingMiddleProductDetailDO::getId));
    }

    default void deleteByRecordId(Long recordId) {
        delete(new LambdaQueryWrapperX<HcGrindingMiddleProductDetailDO>()
                .eq(HcGrindingMiddleProductDetailDO::getRecordId, recordId));
    }

    @Delete("DELETE FROM mes_sfc_grinding_middle_product_detail WHERE record_id = #{recordId}")
    int physicalDeleteByRecordId(@Param("recordId") Long recordId);
}
