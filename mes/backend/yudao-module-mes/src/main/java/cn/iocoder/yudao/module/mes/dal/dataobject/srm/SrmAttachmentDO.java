package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_attachment")
@KeySequence("mes_srm_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmAttachmentDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;
    private Long bizId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String attachmentCategory;
    private String versionGroupNo;
    private Integer versionNo;
    private Long previousAttachmentId;
    private Boolean latestVersion;
    private Long uploadUserId;
    private String uploadUserName;
    private LocalDateTime uploadTime;
    private LocalDateTime versionTime;
    private String updateDescription;
    private String remark;

    private Long tenantId;

}
