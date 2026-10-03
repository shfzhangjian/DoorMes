package cn.iocoder.yudao.module.mes.service.hc.workcenter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产线上下文。
 *
 * <p>业务产线、历史短码、批次号片段必须在这里统一解析，避免各业务模块硬编码。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductionLineContext {

    private Long workCenterId;

    private String workCenterCode;

    private String workCenterName;

    /** 业务产线编码，如 WHITE / BLACK */
    private String lineCode;

    /** 业务产线名称，如白垫线 / 黑垫线 */
    private String lineName;

    /** 产线短码，兼容导布历史数据，如 W / B */
    private String lineShortCode;

    /** 批次号产线码，如白垫量产规则中的 A */
    private String batchLineCode;
}
