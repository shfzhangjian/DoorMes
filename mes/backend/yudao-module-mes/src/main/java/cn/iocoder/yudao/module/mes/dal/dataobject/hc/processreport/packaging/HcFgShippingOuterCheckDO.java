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

/**
 * 发货包装外包装点检与发货需求单的业务关联。
 *
 * 点检项目明细继续由通用过程表单记录承载；本表只负责建立可追溯、可门禁的发货业务关联。
 */
@TableName("mes_inv_fg_shipping_outer_check")
@KeySequence("mes_inv_fg_shipping_outer_check_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgShippingOuterCheckDO extends BaseDO {

    @TableId
    private Long id;

    private Long noticeId;
    private String noticeNo;
    private Long formRecordId;
    private String formCode;
    private Long tenantId;
}
