package cn.iocoder.yudao.module.mes.service.hc.processreport;

/** 压槽研发以设备垫型为准；历史仅使用登记时的设备名称快照。 */
public final class HcPressSlotRndPadType {
    private HcPressSlotRndPadType() {}

    public static String fromName(String name) {
        if (name == null) return null;
        boolean white = name.contains("白垫");
        boolean black = name.contains("黑垫");
        return white == black ? null : white ? "WHITE_PAD" : "BLACK_PAD";
    }

    public static String forEquipment(String configured, String name) {
        String type = configured == null ? "" : configured.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"WHITE_PAD".equals(type) && !"BLACK_PAD".equals(type)) return null;
        String named = fromName(name);
        if (name != null && name.contains("白垫") && name.contains("黑垫")) return null;
        return named != null && !named.equals(type) ? null : type;
    }

    public static String forHistory(String snapshot, String name) {
        if ("WHITE_PAD".equals(snapshot) || "BLACK_PAD".equals(snapshot)) return snapshot;
        String named = fromName(name);
        return named == null ? "UNCLASSIFIED" : named;
    }
}
