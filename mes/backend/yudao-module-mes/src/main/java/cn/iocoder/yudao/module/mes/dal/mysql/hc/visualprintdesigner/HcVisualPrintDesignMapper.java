package cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintDesignDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcVisualPrintDesignMapper extends BaseMapperX<HcVisualPrintDesignDO> {

    default HcVisualPrintDesignDO selectByInfoIdAndKind(Long customerInfoId, Long productItemId, String labelKind) {
        LambdaQueryWrapperX<HcVisualPrintDesignDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcVisualPrintDesignDO::getCustomerInfoId, customerInfoId);
        queryWrapper.eq(HcVisualPrintDesignDO::getLabelKind, labelKind);
        if (productItemId == null) {
            queryWrapper.isNull(HcVisualPrintDesignDO::getProductItemId);
        } else {
            queryWrapper.eq(HcVisualPrintDesignDO::getProductItemId, productItemId);
        }
        queryWrapper.orderByAsc(HcVisualPrintDesignDO::getId).last("LIMIT 1");
        return selectOne(queryWrapper);
    }

    default HcVisualPrintDesignDO selectSharedTemplateByImage(Long customerInfoId, String labelKind, String imageId,
            String imageFile) {
        LambdaQueryWrapperX<HcVisualPrintDesignDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcVisualPrintDesignDO::getCustomerInfoId, customerInfoId);
        queryWrapper.isNull(HcVisualPrintDesignDO::getProductItemId);
        queryWrapper.eq(HcVisualPrintDesignDO::getLabelKind, labelKind);
        if (StrUtil.isBlank(imageId)) {
            queryWrapper.and(wrapper -> wrapper.isNull(HcVisualPrintDesignDO::getImageId)
                    .or().eq(HcVisualPrintDesignDO::getImageId, ""));
        } else {
            queryWrapper.eq(HcVisualPrintDesignDO::getImageId, imageId);
        }
        if (StrUtil.isBlank(imageFile)) {
            queryWrapper.and(wrapper -> wrapper.isNull(HcVisualPrintDesignDO::getImageFile)
                    .or().eq(HcVisualPrintDesignDO::getImageFile, ""));
        } else {
            queryWrapper.eq(HcVisualPrintDesignDO::getImageFile, imageFile);
        }
        return selectOne(queryWrapper.orderByAsc(HcVisualPrintDesignDO::getId).last("LIMIT 1"));
    }

    default List<HcVisualPrintDesignDO> selectListByInfoId(Long customerInfoId, Long productItemId) {
        LambdaQueryWrapperX<HcVisualPrintDesignDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcVisualPrintDesignDO::getCustomerInfoId, customerInfoId);
        if (productItemId == null) {
            queryWrapper.isNull(HcVisualPrintDesignDO::getProductItemId);
        } else {
            queryWrapper.eq(HcVisualPrintDesignDO::getProductItemId, productItemId);
        }
        return selectList(queryWrapper.orderByAsc(HcVisualPrintDesignDO::getLabelKind));
    }

    default List<HcVisualPrintDesignDO> selectListByCustomerInfoId(Long customerInfoId) {
        return selectList(new LambdaQueryWrapperX<HcVisualPrintDesignDO>()
                .eq(HcVisualPrintDesignDO::getCustomerInfoId, customerInfoId)
                .orderByAsc(HcVisualPrintDesignDO::getLabelKind));
    }

    default void physicalDeleteByCustomerInfoId(Long customerInfoId) {
        delete(new LambdaQueryWrapperX<HcVisualPrintDesignDO>()
                .eq(HcVisualPrintDesignDO::getCustomerInfoId, customerInfoId));
    }

    default void physicalDeleteByProductItemId(Long productItemId) {
        delete(new LambdaQueryWrapperX<HcVisualPrintDesignDO>()
                .eq(HcVisualPrintDesignDO::getProductItemId, productItemId));
    }

}
