package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcFgShippingNoticeMapper extends BaseMapperX<HcFgShippingNoticeDO> {

    default PageResult<HcFgShippingNoticeDO> selectPage(ShippingNoticePageReqVO reqVO) {
        LambdaQueryWrapperX<HcFgShippingNoticeDO> queryWrapper = new LambdaQueryWrapperX<>();
        String keyword = StrUtil.trim(reqVO.getKeyword());
        queryWrapper.and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                        .like(HcFgShippingNoticeDO::getNoticeNo, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getCustomerName, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getOrderNo, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getErpOrderNo, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getMaterialCode, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getMaterialName, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getModelCode, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getExternalProductModel, keyword)
                        .or()
                        .like(HcFgShippingNoticeDO::getExternalProductCode, keyword));
        if ("SHIPPING_PACKAGE_ALL".equals(reqVO.getNoticeStatus())) {
            queryWrapper.in(HcFgShippingNoticeDO::getNoticeStatus, List.of("INSPECTED", "OQC_INSPECTING",
                    "OQC_PASSED", "OQC_REJECTED", "PACKAGED", "CLOSED", "SHIPPED"));
            reqVO.setNoticeStatus(null);
        }
        if ("PENDING_OUTER_PACKAGING".equals(reqVO.getNoticeStatus())) {
            queryWrapper.in(HcFgShippingNoticeDO::getNoticeStatus, List.of("OQC_PASSED", "PACKAGED"));
            reqVO.setNoticeStatus(null);
        }
        if ("PENDING_SHIPPING_FQC".equals(reqVO.getNoticeStatus())) {
            queryWrapper.eq(HcFgShippingNoticeDO::getNoticeStatus, "SHIP_CONFIRMED");
            reqVO.setNoticeStatus(null);
        }
        if ("PENDING_PICKING".equals(reqVO.getNoticeStatus())) {
            queryWrapper.and(wrapper -> wrapper
                    .eq(HcFgShippingNoticeDO::getNoticeStatus, "SUBMITTED")
                    .or()
                    .eq(HcFgShippingNoticeDO::getNoticeStatus, "PICKED")
                    .apply("COALESCE(locked_qty, 0) < COALESCE(NULLIF(required_ship_qty, 0), NULLIF(notice_qty, 0), 0)"));
            reqVO.setNoticeStatus(null);
        }
        if ("PICK_COMPLETED".equals(reqVO.getNoticeStatus())) {
            queryWrapper.and(wrapper -> wrapper
                    .in(HcFgShippingNoticeDO::getNoticeStatus, List.of(
                            "SHIP_CONFIRMED", "INSPECTED", "PACKAGED", "LOCKED", "OUTBOUND", "SHIPPED", "CLOSED"))
                    .or()
                    .eq(HcFgShippingNoticeDO::getNoticeStatus, "PICKED")
                    .apply("COALESCE(locked_qty, 0) >= COALESCE(NULLIF(required_ship_qty, 0), NULLIF(notice_qty, 0), 0)"));
            reqVO.setNoticeStatus(null);
        }
        if ("PENDING_OUTBOUND".equals(reqVO.getNoticeStatus())) {
            queryWrapper.in(HcFgShippingNoticeDO::getNoticeStatus, List.of("SUBMITTED", "PICKED", "SHIP_CONFIRMED", "INSPECTED", "PACKAGED", "LOCKED", "OUTBOUND"));
            reqVO.setNoticeStatus(null);
        }
        if ("EXECUTING".equals(reqVO.getNoticeStatus())) {
            queryWrapper.in(HcFgShippingNoticeDO::getNoticeStatus, List.of("SUBMITTED", "PICKED", "SHIP_CONFIRMED", "INSPECTED", "PACKAGED", "LOCKED", "OUTBOUND"));
            reqVO.setNoticeStatus(null);
        }
        if ("CLOSED".equals(reqVO.getNoticeStatus())) {
            queryWrapper.in(HcFgShippingNoticeDO::getNoticeStatus, List.of("CLOSED", "SHIPPED"));
            reqVO.setNoticeStatus(null);
        }
        return selectPage(reqVO, queryWrapper
                .likeIfPresent(HcFgShippingNoticeDO::getNoticeNo, reqVO.getNoticeNo())
                .likeIfPresent(HcFgShippingNoticeDO::getCustomerName, reqVO.getCustomerName())
                .eqIfPresent(HcFgShippingNoticeDO::getProductType, reqVO.getProductType())
                .likeIfPresent(HcFgShippingNoticeDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcFgShippingNoticeDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcFgShippingNoticeDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(HcFgShippingNoticeDO::getNoticeStatus, reqVO.getNoticeStatus())
                .geIfPresent(HcFgShippingNoticeDO::getShippingTime,
                        reqVO.getShippingDateStart() == null ? null : reqVO.getShippingDateStart().atStartOfDay())
                .ltIfPresent(HcFgShippingNoticeDO::getShippingTime,
                        reqVO.getShippingDateEnd() == null ? null : reqVO.getShippingDateEnd().plusDays(1).atStartOfDay())
                .eq(HcFgShippingNoticeDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeDO::getShippingTime)
                .orderByDesc(HcFgShippingNoticeDO::getId));
    }

    default HcFgShippingNoticeDO selectByNoticeNo(String noticeNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticeDO>()
                .eq(HcFgShippingNoticeDO::getNoticeNo, noticeNo)
                .eq(HcFgShippingNoticeDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_inv_fg_shipping_notice
            WHERE id = #{id}
              AND deleted = 0
            FOR UPDATE
            """)
    HcFgShippingNoticeDO selectByIdForUpdate(@Param("id") Long id);

    @Delete("""
            DELETE FROM mes_inv_fg_shipping_notice
            WHERE id = #{id}
            """)
    int physicalDeleteById(@Param("id") Long id);

    @Select("""
            SELECT MAX(CAST(SUBSTRING(notice_no, LENGTH(#{prefix}) + 1) AS UNSIGNED))
            FROM mes_inv_fg_shipping_notice
            WHERE deleted = 0
              AND notice_no LIKE CONCAT(#{prefix}, '%')
            """)
    Integer selectMaxNoticeNoSerial(@Param("prefix") String prefix);

    @Select("""
            SELECT *
            FROM mes_inv_fg_shipping_notice
            WHERE deleted = 0
              AND notice_status IN ('SUBMITTED', 'PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'OQC_INSPECTING',
                  'OQC_PASSED', 'OQC_REJECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR notice_no LIKE CONCAT('%', #{keyword}, '%')
                OR customer_name LIKE CONCAT('%', #{keyword}, '%')
                OR order_no LIKE CONCAT('%', #{keyword}, '%')
                OR erp_order_no LIKE CONCAT('%', #{keyword}, '%')
                OR material_code LIKE CONCAT('%', #{keyword}, '%')
                OR model_code LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY shipping_time ASC, id DESC
            LIMIT 100
            """)
    List<HcFgShippingNoticeDO> selectOutboundNoticeList(@Param("keyword") String keyword);
}
