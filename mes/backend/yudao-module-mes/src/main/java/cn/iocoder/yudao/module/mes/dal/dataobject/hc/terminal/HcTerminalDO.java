package cn.iocoder.yudao.module.mes.dal.dataobject.hc.terminal;

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

/**
 * 终端工位 DO
 */
@TableName("mes_sfc_terminal")
@KeySequence("mes_sfc_terminal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcTerminalDO extends BaseDO {

    /** 终端编码 */
    private String terminalCode;

    /** 终端名称 */
    private String terminalName;

    /** 工作中心ID */
    private Long workCenterId;

    /** 工作中心编码 */
    private String workCenterCode;

    /** 终端模式 */
    private String terminalMode;

    /** 状态 */
    private Integer status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}