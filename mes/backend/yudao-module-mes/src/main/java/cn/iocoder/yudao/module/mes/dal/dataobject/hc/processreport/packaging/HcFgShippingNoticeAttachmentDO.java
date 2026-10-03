package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_inv_fg_shipping_notice_attachment")
@KeySequence("mes_inv_fg_shipping_notice_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgShippingNoticeAttachmentDO extends BaseDO {

    @TableId
    private Long id;

    private Long noticeId;
    private String noticeNo;
    private String attachmentName;
    private String attachmentUrl;
    private String attachmentType;
    private String sourceFileName;
    private String sourceSheetName;
    private Integer sourceSheetIndex;
    private Integer sourceSheetTotal;
    private Long fileSize;
    private String remark;
    private Long tenantId;
}
