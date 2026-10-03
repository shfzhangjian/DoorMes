package cn.iocoder.yudao.module.mes.dal.dataobject.hc.printagentpackage;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("mes_print_agent_package")
@KeySequence("mes_print_agent_package_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPrintAgentPackageDO extends BaseDO {

    @TableId
    private Long id;

    private String packageCode;
    private String versionNo;
    private String packageName;
    private String packageUrl;
    private String packageSha256;
    private Long packageSize;
    private String status;
    private Boolean currentFlag;
    private LocalDateTime publishTime;
    private String releaseNote;
    private String remark;
    private Long tenantId;

}
