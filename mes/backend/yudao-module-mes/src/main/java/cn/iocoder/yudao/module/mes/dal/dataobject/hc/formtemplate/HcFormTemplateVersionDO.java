package cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 表单模板 DO
 */
@TableName("mes_form_template_version")
@KeySequence("mes_form_template_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFormTemplateVersionDO extends BaseDO {

    /** 模板ID */
    private Long templateId;

    /** 模板编码 */
    private String templateCode;

    /** 版本号 */
    private String versionNo;

    /** 是否当前版本 */
    private Boolean isCurrent;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 来源文件名 */
    private String sourceFileName;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}