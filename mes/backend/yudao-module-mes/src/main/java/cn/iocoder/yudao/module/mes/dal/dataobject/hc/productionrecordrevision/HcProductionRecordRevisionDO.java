package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionrecordrevision;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产记录展示修订台账。
 *
 * <p>该表只保存报表展示覆盖值，不回写任何报工、批次或耗材事实表。</p>
 */
@TableName("mes_hc_production_record_revision")
@KeySequence("mes_hc_production_record_revision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcProductionRecordRevisionDO extends BaseDO {

    @TableId
    private Long id;

    private String moduleCode;

    private String recordKey;

    private String originalSnapshotJson;

    private String revisedDataJson;

    private String reviseReason;

    private Integer revisionNo;

    private Long reviseUserId;

    private String reviseUserName;

    private LocalDateTime revisedAt;

    private Long tenantId;

}
