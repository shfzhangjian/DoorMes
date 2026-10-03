package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierMaskFieldDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierMaskFieldMapper extends BaseMapperX<SrmSupplierMaskFieldDO> {

    default List<SrmSupplierMaskFieldDO> selectOrderedList() {
        return selectList(new LambdaQueryWrapperX<SrmSupplierMaskFieldDO>()
                .orderByAsc(SrmSupplierMaskFieldDO::getSort)
                .orderByAsc(SrmSupplierMaskFieldDO::getId));
    }

    default SrmSupplierMaskFieldDO selectByFieldKey(String fieldKey) {
        return selectOne(SrmSupplierMaskFieldDO::getFieldKey, fieldKey);
    }

}
