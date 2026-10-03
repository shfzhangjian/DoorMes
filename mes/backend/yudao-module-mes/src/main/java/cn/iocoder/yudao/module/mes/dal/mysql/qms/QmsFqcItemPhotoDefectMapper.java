package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemPhotoDefectDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QmsFqcItemPhotoDefectMapper extends BaseMapperX<QmsFqcItemPhotoDefectDO> {

    default List<QmsFqcItemPhotoDefectDO> selectListByPhotoIds(Collection<Long> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcItemPhotoDefectDO>()
                .in(QmsFqcItemPhotoDefectDO::getItemPhotoId, photoIds)
                .orderByAsc(QmsFqcItemPhotoDefectDO::getItemPhotoId)
                .orderByAsc(QmsFqcItemPhotoDefectDO::getSort)
                .orderByAsc(QmsFqcItemPhotoDefectDO::getId));
    }

    default List<QmsFqcItemPhotoDefectDO> selectListByItem(Long fqcId, Long fqcItemId) {
        return selectList(new LambdaQueryWrapper<QmsFqcItemPhotoDefectDO>()
                .eq(QmsFqcItemPhotoDefectDO::getFqcId, fqcId)
                .eq(QmsFqcItemPhotoDefectDO::getFqcItemId, fqcItemId)
                .orderByAsc(QmsFqcItemPhotoDefectDO::getSort)
                .orderByAsc(QmsFqcItemPhotoDefectDO::getId));
    }

    @Delete("DELETE FROM mes_qms_fqc_item_photo_defect WHERE item_photo_id = #{photoId}")
    int deletePhysicallyByPhotoId(@Param("photoId") Long photoId);
}
