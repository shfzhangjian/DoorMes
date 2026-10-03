package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeAttachmentDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcFgShippingNoticeAttachmentMapper extends BaseMapperX<HcFgShippingNoticeAttachmentDO> {

    default List<HcFgShippingNoticeAttachmentDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticeAttachmentDO>()
                .eq(HcFgShippingNoticeAttachmentDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticeAttachmentDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticeAttachmentDO::getId));
    }

    @Select("""
            SELECT COUNT(1)
            FROM mes_inv_fg_shipping_notice_attachment
            WHERE deleted = b'0'
              AND attachment_type = #{attachmentType}
              AND (source_file_name = #{fileName} OR attachment_name = #{fileName})
            """)
    Long countByImportFileName(@Param("attachmentType") String attachmentType,
                               @Param("fileName") String fileName);

    @Select("""
            SELECT *
            FROM mes_inv_fg_shipping_notice_attachment
            WHERE deleted = b'0'
              AND attachment_type = #{attachmentType}
              AND (source_file_name = #{fileName} OR attachment_name = #{fileName})
            ORDER BY COALESCE(source_sheet_index, 999999), id
            """)
    List<HcFgShippingNoticeAttachmentDO> selectImportListByFileName(@Param("attachmentType") String attachmentType,
                                                                    @Param("fileName") String fileName);

    @Delete("""
            DELETE FROM mes_inv_fg_shipping_notice_attachment
            WHERE notice_id = #{noticeId}
            """)
    int physicalDeleteByNoticeId(@Param("noticeId") Long noticeId);
}
