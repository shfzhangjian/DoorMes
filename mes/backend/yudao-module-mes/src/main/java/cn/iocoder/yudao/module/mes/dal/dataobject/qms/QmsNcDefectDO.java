package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_nc_defect")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcDefectDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ncRecordId;
    private String ncNo;
    private Long defectCodeId;
    private String defectCode;
    private String defectName;
    private String defectPath;
    private String sourceSectionName;
    private String sourceInspectionItem;
    private String sourceResult;
    private Boolean primaryFlag;
    private Integer sort;
    private Long tenantId;
}
