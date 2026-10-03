package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstAllocationDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcGrindingFirstAllocationMapper extends BaseMapperX<HcGrindingFirstAllocationDO> {

    default List<HcGrindingFirstAllocationDO> selectListByModeId(Long allocationModeId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingFirstAllocationDO>()
                .eq(HcGrindingFirstAllocationDO::getAllocationModeId, allocationModeId)
                .eq(HcGrindingFirstAllocationDO::getDeleted, false)
                .orderByAsc(HcGrindingFirstAllocationDO::getStartPosition)
                .orderByAsc(HcGrindingFirstAllocationDO::getId));
    }

    default HcGrindingFirstAllocationDO selectBySegment(Long allocationModeId, String segmentMark) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingFirstAllocationDO>()
                .eq(HcGrindingFirstAllocationDO::getAllocationModeId, allocationModeId)
                .eq(HcGrindingFirstAllocationDO::getSegmentMark, segmentMark)
                .eq(HcGrindingFirstAllocationDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcGrindingFirstAllocationDO selectByFirstDetailId(Long firstDetailId) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingFirstAllocationDO>()
                .eq(HcGrindingFirstAllocationDO::getFirstDetailId, firstDetailId)
                .eq(HcGrindingFirstAllocationDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_grinding_first_allocation_detail
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    HcGrindingFirstAllocationDO selectByIdForUpdate(@Param("id") Long id);

    @Delete("DELETE FROM mes_sfc_grinding_first_allocation_detail WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
