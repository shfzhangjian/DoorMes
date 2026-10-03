package cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintLabelBindingDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcVisualPrintLabelBindingMapper extends BaseMapperX<HcVisualPrintLabelBindingDO> {

    default HcVisualPrintLabelBindingDO selectByProductItemIdAndKind(Long productItemId, String labelKind) {
        return selectOne(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getProductItemId, productItemId)
                .eq(HcVisualPrintLabelBindingDO::getLabelKind, labelKind));
    }

    default List<HcVisualPrintLabelBindingDO> selectListByProductItemId(Long productItemId) {
        return selectList(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getProductItemId, productItemId)
                .orderByAsc(HcVisualPrintLabelBindingDO::getLabelKind));
    }

    default List<HcVisualPrintLabelBindingDO> selectListByDesignId(Long designId) {
        return selectList(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getDesignId, designId)
                .orderByAsc(HcVisualPrintLabelBindingDO::getProductItemId));
    }

    default List<HcVisualPrintLabelBindingDO> selectListByTemplateKey(Long customerInfoId, String labelKind,
            String imageId, String imageFile) {
        LambdaQueryWrapperX<HcVisualPrintLabelBindingDO> queryWrapper = new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getCustomerInfoId, customerInfoId)
                .eq(HcVisualPrintLabelBindingDO::getLabelKind, labelKind);
        if (imageId == null || imageId.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.isNull(HcVisualPrintLabelBindingDO::getImageId)
                    .or().eq(HcVisualPrintLabelBindingDO::getImageId, ""));
        } else {
            queryWrapper.eq(HcVisualPrintLabelBindingDO::getImageId, imageId);
        }
        if (imageFile == null || imageFile.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.isNull(HcVisualPrintLabelBindingDO::getImageFile)
                    .or().eq(HcVisualPrintLabelBindingDO::getImageFile, ""));
        } else {
            queryWrapper.eq(HcVisualPrintLabelBindingDO::getImageFile, imageFile);
        }
        return selectList(queryWrapper.orderByAsc(HcVisualPrintLabelBindingDO::getProductItemId));
    }

    default Long selectCountByDesignId(Long designId) {
        return selectCount(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getDesignId, designId));
    }

    default void physicalDeleteByCustomerInfoId(Long customerInfoId) {
        delete(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getCustomerInfoId, customerInfoId));
    }

    default void physicalDeleteByProductItemId(Long productItemId) {
        delete(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getProductItemId, productItemId));
    }

    default void physicalDeleteByProductItemIdAndKind(Long productItemId, String labelKind) {
        delete(new LambdaQueryWrapperX<HcVisualPrintLabelBindingDO>()
                .eq(HcVisualPrintLabelBindingDO::getProductItemId, productItemId)
                .eq(HcVisualPrintLabelBindingDO::getLabelKind, labelKind));
    }

}
