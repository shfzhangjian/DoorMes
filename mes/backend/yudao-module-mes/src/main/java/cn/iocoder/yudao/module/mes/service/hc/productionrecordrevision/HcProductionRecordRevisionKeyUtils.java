package cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 生产记录展示聚合行的稳定标识生成工具。
 *
 * <p>部分生产记录由多张来源表聚合得到，不能直接复用任一来源表主键。使用业务聚合键生成稳定编号，
 * 使展示修订能够在后续查询时精确回载。</p>
 */
public final class HcProductionRecordRevisionKeyUtils {

    private HcProductionRecordRevisionKeyUtils() {
    }

    public static Long generateDisplayId(String moduleCode, Object aggregationKey) {
        UUID uuid = UUID.nameUUIDFromBytes((moduleCode + ":" + aggregationKey)
                .getBytes(StandardCharsets.UTF_8));
        long value = uuid.getMostSignificantBits() & Long.MAX_VALUE;
        return value == 0L ? 1L : value;
    }

}
