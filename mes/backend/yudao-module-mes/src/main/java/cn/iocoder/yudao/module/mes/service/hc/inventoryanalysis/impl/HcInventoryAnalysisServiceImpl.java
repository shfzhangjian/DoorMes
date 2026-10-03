package cn.iocoder.yudao.module.mes.service.hc.inventoryanalysis.impl;

import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO.DistributionItem;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO.MovementTrendItem;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO.Summary;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgLayerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgRackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcFgLayerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcFgRackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockTxnLogMapper;
import cn.iocoder.yudao.module.mes.service.hc.inventoryanalysis.HcInventoryAnalysisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成品库位库存分析服务。
 *
 * <p>当前库存严格复用“成品库位管理”的有效库位和在位状态口径；不读取统一库存、WIP 或 NG 库。</p>
 */
@Service
@Validated
public class HcInventoryAnalysisServiceImpl implements HcInventoryAnalysisService {

    private static final String FG_LOCATION_SCENE = "PACKAGE_FG";
    private static final String STATUS_INBOUNDED = "INBOUNDED";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_OUTBOUND_LOCKED = "OUTBOUND_LOCKED";
    private static final String STATUS_ALLOCATED = "ALLOCATED";
    private static final String QUALITY_OK = "OK";
    private static final String QUALITY_NG = "NG";
    private static final int DEFAULT_TOP_N = 20;
    private static final int MAX_QUERY_DAYS = 366;

    @Resource
    private HcLocationMapper hcLocationMapper;
    @Resource
    private HcFgRackMapper hcFgRackMapper;
    @Resource
    private HcFgLayerMapper hcFgLayerMapper;
    @Resource
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Resource
    private HcFinishedStockTxnLogMapper hcFinishedStockTxnLogMapper;

    @Override
    public HcInventoryAnalysisOverviewRespVO getOverview(HcInventoryAnalysisOverviewReqVO reqVO) {
        normalizeQuery(reqVO);
        AnalysisScope scope = loadAnalysisScope();
        List<StockContext> stocks = hcFinishedStockMapper.selectInventoryAnalysisActiveListByLocationCodes(
                        scope.locationCodes(), reqVO.getStartDate(), reqVO.getEndDate())
                .stream()
                .map(stock -> toStockContext(stock, scope))
                .filter(Objects::nonNull)
                .filter(context -> isPositiveQty(context.stock()))
                .filter(context -> matchesCurrentStock(context, reqVO))
                .toList();

        HcInventoryAnalysisOverviewRespVO respVO = new HcInventoryAnalysisOverviewRespVO();
        respVO.setDimension(reqVO.getDimension());
        respVO.setSummary(toSummary(stocks));
        respVO.setDistributionBars(limitDistribution(stocks, reqVO.getDimension(), reqVO.getTopN()));
        respVO.setMovementTrend(toMovementTrend(scope.locationCodeSet(), reqVO));
        return respVO;
    }

    private AnalysisScope loadAnalysisScope() {
        List<HcLocationDO> locations = hcLocationMapper.selectListByBizScene(FG_LOCATION_SCENE);
        Map<String, HcLocationDO> locationByCode = locations.stream()
                .filter(location -> hasText(location.getLocationCode()))
                .collect(Collectors.toMap(HcLocationDO::getLocationCode, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
        Set<Long> warehouseIds = locations.stream().map(HcLocationDO::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<HcFgRackDO> racks = warehouseIds.stream()
                .flatMap(warehouseId -> hcFgRackMapper.selectListByWarehouseId(warehouseId).stream())
                .toList();
        Map<Long, HcFgRackDO> rackById = racks.stream().collect(Collectors.toMap(HcFgRackDO::getId,
                Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<HcFgLayerDO> layers = rackById.isEmpty() ? List.of()
                : hcFgLayerMapper.selectListByRackIds(rackById.keySet());
        Map<Long, HcFgLayerDO> layerById = layers.stream().collect(Collectors.toMap(HcFgLayerDO::getId,
                Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        return new AnalysisScope(locationByCode, rackById, layerById);
    }

    private static StockContext toStockContext(HcFinishedStockDO stock, AnalysisScope scope) {
        HcLocationDO location = scope.locationByCode().get(stock.getLocationCode());
        if (location == null) {
            return null;
        }
        return new StockContext(stock, location, scope.rackById().get(location.getRackId()),
                scope.layerById().get(location.getLayerId()));
    }

    private static void normalizeQuery(HcInventoryAnalysisOverviewReqVO reqVO) {
        LocalDate endDate = reqVO.getEndDate() == null ? LocalDate.now() : reqVO.getEndDate();
        LocalDate startDate = reqVO.getStartDate() == null ? endDate.minusDays(29) : reqVO.getStartDate();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("统计开始日期不能晚于统计结束日期");
        }
        if (startDate.plusDays(MAX_QUERY_DAYS - 1L).isBefore(endDate)) {
            throw new IllegalArgumentException("统计日期范围不能超过 " + MAX_QUERY_DAYS + " 天");
        }
        reqVO.setStartDate(startDate);
        reqVO.setEndDate(endDate);
        reqVO.setDimension(normalizeUpper(reqVO.getDimension(), "MODEL"));
        reqVO.setGranularity(normalizeUpper(reqVO.getGranularity(), "DAY"));
        reqVO.setTopN(reqVO.getTopN() == null ? DEFAULT_TOP_N : reqVO.getTopN());
        reqVO.setQualityStatus(normalizeUpper(reqVO.getQualityStatus(), null));
        reqVO.setStockStatus(normalizeUpper(reqVO.getStockStatus(), null));
    }

    private static String normalizeUpper(String value, String defaultValue) {
        return !hasText(value) ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean matchesCurrentStock(StockContext context, HcInventoryAnalysisOverviewReqVO reqVO) {
        HcFinishedStockDO stock = context.stock();
        return matchesText(firstText(stock.getWarehouseCode(), context.location().getWarehouseCode()), reqVO.getWarehouseCode())
                && matchesText(stock.getLocationCode(), reqVO.getLocationCode())
                && matchesText(stock.getModelCode(), reqVO.getModelCode())
                && matchesText(stock.getBatchNo(), reqVO.getBatchNo())
                && matchesText(stock.getSliceBatchNo(), reqVO.getSliceBatchNo())
                && matchesCode(stock.getQualityStatus(), reqVO.getQualityStatus())
                && matchesCode(stock.getStockStatus(), reqVO.getStockStatus());
    }

    private static Summary toSummary(List<StockContext> contexts) {
        StockAggregate aggregate = aggregate(contexts);
        Summary summary = new Summary();
        summary.setRecordCount(aggregate.recordCount());
        summary.setOccupiedLocationCount(contexts.stream().map(context -> context.stock().getLocationCode())
                .filter(HcInventoryAnalysisServiceImpl::hasText).distinct().count());
        summary.setOnHandQty(aggregate.onHandQty());
        summary.setQualifiedQty(aggregate.qualifiedQty());
        summary.setUnqualifiedQty(aggregate.unqualifiedQty());
        summary.setShippableQty(aggregate.shippableQty());
        summary.setLockedQty(aggregate.lockedQty());
        return summary;
    }

    private static List<DistributionItem> limitDistribution(List<StockContext> contexts, String dimension, int topN) {
        Map<String, DistributionAggregate> grouped = new LinkedHashMap<>();
        contexts.forEach(context -> {
            DimensionValue dimensionValue = resolveDimension(context, dimension);
            DistributionAggregate aggregate = grouped.computeIfAbsent(dimensionValue.key(),
                    ignored -> new DistributionAggregate(dimensionValue, dimension));
            aggregate.add(context);
        });
        List<DistributionItem> items = grouped.values().stream()
                .map(DistributionAggregate::toRespVO)
                .sorted(Comparator.comparing(DistributionItem::getOnHandQty).reversed()
                        .thenComparing(DistributionItem::getDimensionName))
                .toList();
        if (items.size() <= topN) {
            return items;
        }
        List<DistributionItem> result = new ArrayList<>(items.subList(0, topN));
        result.add(buildOtherItem(dimension, items.subList(topN, items.size())));
        return result;
    }

    private static DistributionItem buildOtherItem(String dimension, List<DistributionItem> otherItems) {
        DistributionItem item = new DistributionItem();
        item.setDimension(dimension);
        item.setDimensionKey("__OTHER__");
        item.setDimensionName("其他（" + otherItems.size() + "项）");
        item.setOther(true);
        item.setRecordCount(otherItems.stream().map(DistributionItem::getRecordCount).reduce(0L, Long::sum));
        item.setOnHandQty(sumDistribution(otherItems, DistributionItem::getOnHandQty));
        item.setShippableQty(sumDistribution(otherItems, DistributionItem::getShippableQty));
        item.setLockedQty(sumDistribution(otherItems, DistributionItem::getLockedQty));
        return item;
    }

    private static BigDecimal sumDistribution(List<DistributionItem> items,
                                              Function<DistributionItem, BigDecimal> getter) {
        return items.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<MovementTrendItem> toMovementTrend(Set<String> locationCodes,
                                                     HcInventoryAnalysisOverviewReqVO reqVO) {
        Map<String, TrendAggregate> grouped = new TreeMap<>();
        hcFinishedStockTxnLogMapper.selectInventoryAnalysisTxnList(reqVO).stream()
                .filter(txn -> locationCodes.contains(txn.getLocationCode()))
                .filter(txn -> matchesTxn(txn, reqVO))
                .filter(txn -> isPositiveQty(txn.getQty()))
                .filter(txn -> txn.getTxnTime() != null)
                .forEach(txn -> grouped.computeIfAbsent(toPeriodLabel(txn.getTxnTime(), reqVO.getGranularity()),
                        ignored -> new TrendAggregate()).add(txn));
        return grouped.entrySet().stream().map(entry -> entry.getValue().toRespVO(entry.getKey())).toList();
    }

    private static boolean matchesTxn(HcFinishedStockTxnLogDO txn, HcInventoryAnalysisOverviewReqVO reqVO) {
        return matchesText(txn.getWarehouseCode(), reqVO.getWarehouseCode())
                && matchesText(txn.getLocationCode(), reqVO.getLocationCode())
                && matchesText(txn.getModelCode(), reqVO.getModelCode())
                && matchesText(txn.getBatchNo(), reqVO.getBatchNo())
                && matchesText(txn.getSliceBatchNo(), reqVO.getSliceBatchNo())
                && matchesCode(txn.getQualityStatus(), reqVO.getQualityStatus());
    }

    private static String toPeriodLabel(LocalDateTime txnTime, String granularity) {
        return switch (granularity) {
            case "DAY" -> txnTime.toLocalDate().toString();
            case "MONTH" -> String.format(Locale.ROOT, "%d-%02d", txnTime.getYear(), txnTime.getMonthValue());
            case "WEEK" -> String.format(Locale.ROOT, "%d-W%02d", txnTime.get(WeekFields.ISO.weekBasedYear()),
                    txnTime.get(WeekFields.ISO.weekOfWeekBasedYear()));
            default -> throw new IllegalArgumentException("不支持的趋势统计粒度：" + granularity);
        };
    }

    private static DimensionValue resolveDimension(StockContext context, String dimension) {
        HcFinishedStockDO stock = context.stock();
        HcLocationDO location = context.location();
        return switch (dimension) {
            case "WAREHOUSE" -> value(firstText(stock.getWarehouseCode(), location.getWarehouseCode()),
                    firstText(stock.getWarehouseName(), location.getWarehouseName(), stock.getWarehouseCode()));
            case "LOCATION" -> value(location.getLocationCode(), joinValues(location.getLocationCode(), location.getLocationName()));
            case "RACK" -> value(context.rack() == null ? null : String.valueOf(context.rack().getId()),
                    joinValues(firstText(stock.getWarehouseName(), location.getWarehouseName()), rackName(context.rack())));
            case "LAYER" -> value(context.layer() == null ? null : String.valueOf(context.layer().getId()),
                    joinValues(rackName(context.rack()), layerName(context.layer())));
            case "MODEL" -> value(stock.getModelCode(), stock.getModelCode());
            case "SEGMENT_BATCH" -> value(stock.getBatchNo(), stock.getBatchNo());
            case "SLICE_BATCH" -> value(stock.getSliceBatchNo(), stock.getSliceBatchNo());
            case "MATERIAL" -> value(stock.getMaterialCode(), joinValues(stock.getMaterialCode(), stock.getMaterialName()));
            case "QUALITY_STATUS" -> value(normalizeUpper(stock.getQualityStatus(), null), normalizeUpper(stock.getQualityStatus(), null));
            case "STOCK_STATUS" -> value(normalizeUpper(stock.getStockStatus(), null), normalizeUpper(stock.getStockStatus(), null));
            default -> throw new IllegalArgumentException("不支持的成品库位库存分析维度：" + dimension);
        };
    }

    private static DimensionValue value(String key, String name) {
        String defaultValue = "未填写";
        return new DimensionValue(hasText(key) ? key : defaultValue, hasText(name) ? name : defaultValue);
    }

    private static String rackName(HcFgRackDO rack) {
        return rack == null ? null : firstText(rack.getRackName(), rack.getRackNo() == null ? null : rack.getRackNo() + "#货架");
    }

    private static String layerName(HcFgLayerDO layer) {
        return layer == null ? null : firstText(layer.getLayerName(), layer.getLayerNo() == null ? null : "L" + layer.getLayerNo() + "层");
    }

    private static StockAggregate aggregate(List<StockContext> contexts) {
        StockAggregate result = new StockAggregate();
        contexts.forEach(result::add);
        return result;
    }

    private static boolean isPositiveQty(HcFinishedStockDO stock) {
        return isPositiveQty(stock.getQty());
    }

    private static boolean isPositiveQty(Integer qty) {
        return qty != null && qty > 0;
    }

    private static BigDecimal qty(Integer qty) {
        return BigDecimal.valueOf(qty == null ? 0 : qty);
    }

    private static boolean isShippable(HcFinishedStockDO stock) {
        return List.of(STATUS_INBOUNDED, STATUS_AVAILABLE).contains(normalizeUpper(stock.getStockStatus(), null))
                && isQualified(stock);
    }

    private static boolean isQualified(HcFinishedStockDO stock) {
        return QUALITY_OK.equals(normalizeUpper(stock.getQualityStatus(), null));
    }

    private static boolean isUnqualified(HcFinishedStockDO stock) {
        return QUALITY_NG.equals(normalizeUpper(stock.getQualityStatus(), null));
    }

    private static boolean isLocked(HcFinishedStockDO stock) {
        return List.of(STATUS_OUTBOUND_LOCKED, STATUS_ALLOCATED).contains(normalizeUpper(stock.getStockStatus(), null));
    }

    private static boolean matchesText(String actual, String expected) {
        return !hasText(expected) || hasText(actual)
                && actual.toUpperCase(Locale.ROOT).contains(expected.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean matchesCode(String actual, String expected) {
        return !hasText(expected) || expected.trim().equalsIgnoreCase(actual);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String joinValues(String... values) {
        return java.util.Arrays.stream(values).filter(HcInventoryAnalysisServiceImpl::hasText)
                .map(String::trim).distinct().collect(Collectors.joining(" / "));
    }

    private record AnalysisScope(Map<String, HcLocationDO> locationByCode,
                                 Map<Long, HcFgRackDO> rackById,
                                 Map<Long, HcFgLayerDO> layerById) {

        List<String> locationCodes() {
            return List.copyOf(locationByCode.keySet());
        }

        Set<String> locationCodeSet() {
            return locationByCode.keySet();
        }
    }

    private record StockContext(HcFinishedStockDO stock, HcLocationDO location,
                                HcFgRackDO rack, HcFgLayerDO layer) {
    }

    private record DimensionValue(String key, String name) {
    }

    private static final class StockAggregate {

        private long recordCount;
        private BigDecimal onHandQty = BigDecimal.ZERO;
        private BigDecimal qualifiedQty = BigDecimal.ZERO;
        private BigDecimal unqualifiedQty = BigDecimal.ZERO;
        private BigDecimal shippableQty = BigDecimal.ZERO;
        private BigDecimal lockedQty = BigDecimal.ZERO;

        private void add(StockContext context) {
            BigDecimal stockQty = qty(context.stock().getQty());
            recordCount++;
            onHandQty = onHandQty.add(stockQty);
            if (isQualified(context.stock())) {
                qualifiedQty = qualifiedQty.add(stockQty);
            }
            if (isUnqualified(context.stock())) {
                unqualifiedQty = unqualifiedQty.add(stockQty);
            }
            if (isShippable(context.stock())) {
                shippableQty = shippableQty.add(stockQty);
            }
            if (isLocked(context.stock())) {
                lockedQty = lockedQty.add(stockQty);
            }
        }

        private long recordCount() {
            return recordCount;
        }

        private BigDecimal onHandQty() {
            return onHandQty;
        }

        private BigDecimal qualifiedQty() {
            return qualifiedQty;
        }

        private BigDecimal unqualifiedQty() {
            return unqualifiedQty;
        }

        private BigDecimal shippableQty() {
            return shippableQty;
        }

        private BigDecimal lockedQty() {
            return lockedQty;
        }
    }

    private static final class DistributionAggregate {

        private final DimensionValue dimension;
        private final String dimensionCode;
        private final StockAggregate aggregate = new StockAggregate();

        private DistributionAggregate(DimensionValue dimension, String dimensionCode) {
            this.dimension = dimension;
            this.dimensionCode = dimensionCode;
        }

        private void add(StockContext context) {
            aggregate.add(context);
        }

        private DistributionItem toRespVO() {
            DistributionItem item = new DistributionItem();
            item.setDimension(dimensionCode);
            item.setDimensionKey(dimension.key());
            item.setDimensionName(dimension.name());
            item.setOther(false);
            item.setRecordCount(aggregate.recordCount());
            item.setOnHandQty(aggregate.onHandQty());
            item.setShippableQty(aggregate.shippableQty());
            item.setLockedQty(aggregate.lockedQty());
            return item;
        }
    }

    private static final class TrendAggregate {

        private BigDecimal inboundQty = BigDecimal.ZERO;
        private BigDecimal outboundQty = BigDecimal.ZERO;
        private BigDecimal repackReturnQty = BigDecimal.ZERO;

        private void add(HcFinishedStockTxnLogDO txn) {
            BigDecimal txnQty = qty(txn.getQty());
            switch (txn.getTxnType()) {
                case "FG_INBOUND" -> inboundQty = inboundQty.add(txnQty);
                case "FG_MANUAL_OUTBOUND", "FG_SHIP" -> outboundQty = outboundQty.add(txnQty);
                case "FG_PACKAGE_SPLIT_RETURN", "FG_SHIPPING_RETURN" -> repackReturnQty = repackReturnQty.add(txnQty);
                default -> {
                    // Mapper 已限制交易类型；保留分支使历史异常值不影响其他事实。
                }
            }
        }

        private MovementTrendItem toRespVO(String periodLabel) {
            MovementTrendItem item = new MovementTrendItem();
            item.setPeriodLabel(periodLabel);
            item.setInboundQty(inboundQty);
            item.setOutboundQty(outboundQty);
            item.setRepackReturnQty(repackReturnQty);
            item.setNetChangeQty(inboundQty.subtract(outboundQty).subtract(repackReturnQty));
            return item;
        }
    }

}
