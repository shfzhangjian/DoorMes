package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

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

@TableName("mes_qms_fqc_sheet_section")
@KeySequence("mes_qms_fqc_sheet_section_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcSheetSectionDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;

    private String sectionCode;

    private String sectionName;

    private String sectionType;

    private Integer expectedRows;

    private Integer expectedColumns;

    private String excelRange;

    private Integer sort;

    private Long tenantId;
}
