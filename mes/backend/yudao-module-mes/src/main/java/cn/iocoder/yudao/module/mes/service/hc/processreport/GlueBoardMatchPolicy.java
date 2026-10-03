package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import java.util.List;
import java.util.stream.Collectors;

/** 粘胶1/2共用的严格用料规则；空配置不能作为通配条件。 */
final class GlueBoardMatchPolicy {
    private GlueBoardMatchPolicy() { }

    static String mismatch(String productModel, String boardModel, String materialCode,
                           List<HcFinishedGlueBoardMapItemDO> candidates) {
        if (StrUtil.isBlank(productModel)) {
            return "未取得实际生产型号，不能保存或扫码确认报工";
        }
        if (candidates == null || candidates.isEmpty()) {
            return "产品型号 " + productModel + " 未维护当前工序的有效胶板映射，不能保存或扫码确认报工";
        }
        if (candidates.stream().anyMatch(item -> StrUtil.isBlank(item.getGlueBoardModel())
                || StrUtil.isBlank(item.getGlueBoardMaterialCode()))) {
            return "产品型号 " + productModel + " 的胶板映射型号或料号不完整，请维护后再报工";
        }
        if (StrUtil.isBlank(boardModel) || StrUtil.isBlank(materialCode)) {
            return "实际胶板型号或料号为空，不能保存或扫码确认报工";
        }
        if (candidates.stream().anyMatch(item -> same(item.getGlueBoardModel(), boardModel)
                && same(item.getGlueBoardMaterialCode(), materialCode))) {
            return null;
        }
        String allowed = candidates.stream().map(item -> item.getGlueBoardModel() + "（"
                + item.getGlueBoardMaterialCode() + "）").distinct().collect(Collectors.joining("、"));
        return "当前产品型号 " + productModel + " 允许使用胶板 " + allowed + "，实际胶板 "
                + boardModel + "（" + materialCode + "）不匹配，不能保存或扫码确认报工";
    }

    static boolean same(String left, String right) {
        return StrUtil.isNotBlank(left) && StrUtil.isNotBlank(right)
                && StrUtil.equalsIgnoreCase(left.trim(), right.trim());
    }
}
