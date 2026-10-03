// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/MesTestConstants.java
package cn.iocoder.yudao.module.mes.ut;

/**
 * MES 测试通用常量定义
 * 用途：确保所有测试类引用一致的租户ID，避免硬编码
 */
public class MesTestConstants {

    /**
     * 租户 T01: 禾臣新材料 (Film)
     * 业务模式: 混合制造 (配料-涂布-分切-模切)
     * 对应 CSV: system_tenant.csv -> ID 121
     */
    public static final Long TENANT_ID_FILM = 121L;

    /**
     * 租户 T02: 芜湖捷和科技 (Kejie)
     * 业务模式: 离散制造 (压铸-CNC-注塑-组装)
     * 对应 CSV: system_tenant.csv -> ID 1
     */
    public static final Long TENANT_ID_KEJIE = 1L;

}
