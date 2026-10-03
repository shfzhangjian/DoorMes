package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemPhotoDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcItemPhotoMapper extends BaseMapperX<QmsFqcItemPhotoDO> {

    default List<QmsFqcItemPhotoDO> selectListByItem(Long fqcId, Long submissionDetailId, Long fqcItemId) {
        if (fqcId == null || submissionDetailId == null || fqcItemId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcItemPhotoDO>()
                .eq(QmsFqcItemPhotoDO::getFqcId, fqcId)
                .eq(QmsFqcItemPhotoDO::getSubmissionDetailId, submissionDetailId)
                .eq(QmsFqcItemPhotoDO::getFqcItemId, fqcItemId)
                .orderByAsc(QmsFqcItemPhotoDO::getSort)
                .orderByAsc(QmsFqcItemPhotoDO::getId));
    }
}
