package cn.iocoder.yudao.module.mes.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 派工细单状态枚举
 * 对应数据库字段: mes_work_order_sub.status
 */
@Getter
@AllArgsConstructor
public enum SubOrderStatusEnum {

    PENDING("PENDING", "待开工"),
    DOING("DOING", "执行中"),
    SUSPENDED("SUSPENDED", "缺料挂起"),
    FROZEN("FROZEN", "异常冻结"),
    DONE("DONE", "完工");

    private final String status;
    private final String name;

    /**
     * 校验状态流转是否合法 (简单状态机)
     *
     * @param currentStatus 当前状态
     * @param targetStatus  目标状态
     * @return true=允许流转
     */
    public static boolean isTransitionAllowed(String currentStatus, String targetStatus) {
        if (Objects.equals(currentStatus, targetStatus)) {
            return true;
        }
        // 如果当前状态为空，默认允许设置任意状态（初始化）
        if (currentStatus == null) {
            return true;
        }

        return switch (targetStatus) {
            case "DOING" -> "PENDING".equals(currentStatus) || "SUSPENDED".equals(currentStatus);
            case "SUSPENDED", "FROZEN" -> "DOING".equals(currentStatus); // 只能从执行中挂起或冻结
            case "DONE" -> "DOING".equals(currentStatus); // 只能从执行中完工
            default -> false;
        };
    }

    public static boolean isValid(String status) {
        return Arrays.stream(values()).anyMatch(e -> e.getStatus().equals(status));
    }
}
