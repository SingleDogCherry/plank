package com.mistra.plank.job;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.enums.AutomaticTradingEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author rui.wang
 * @ Version: 1.0
 * @ Time: 2023/2/15 13:17
 * @ Description: 自动打板交易，开启的话会监控成交额大于3亿(PlankConfig.stockTurnoverFilter)的全市场股票，发现上板则会自动下单排队
 * 只打首板，反包板。即昨日没有上板的股票
 * 昨日连板的股票我想的是自己过滤之后再选择打哪个，就通过AutomaticTrading提前配置来打板
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "plank", name = "automaticPlankTrading", havingValue = "true")
public class AutomaticPlankTrading implements CommandLineRunner {

    private final PlankConfig plankConfig;
    private final StockProcessor stockProcessor;

    private final AutomaticTrading automaticTrading;

    private final StockMapper stockMapper;
    /**
     * 自动打板股票，二级过滤map
     * 主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，上板则下单排队
     */
    private static final ConcurrentHashSet<String> PLANK_MONITOR = new ConcurrentHashSet<>();

    public AutomaticPlankTrading(PlankConfig plankConfig, StockProcessor stockProcessor,
                                 AutomaticTrading automaticTrading, StockMapper stockMapper) {
        this.plankConfig = plankConfig;
        this.stockProcessor = stockProcessor;
        this.automaticTrading = automaticTrading;
        this.stockMapper = stockMapper;
    }

    /**
     * 每5秒过滤主板涨幅大于7个点股票，创业板涨幅大于18个点的股票，放入PLANK_MONITOR
     */
    @Scheduled(cron = "*/5 * * * * ?")
    private void filterStock() {
        if (openAutoPlank()) {
            List<List<String>> lists = Lists.partition(Lists.newArrayList(Barbarossa.STOCK_FILTER_MAP.keySet()),
                    Barbarossa.executorService.getMaximumPoolSize() / 2);
            for (List<String> list : lists) {
                Barbarossa.executorService.submit(() -> filterStock(list));
            }
            log.warn("当前打板监测股票:{}", PLANK_MONITOR);
        }
    }

    /**
     * 过滤涨幅大于7个点股票
     *
     * @param codes codes
     */
    private void filterStock(List<String> codes) {
        codes.forEach(e -> {
            StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(e);
            if ((stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() > 18) ||
                    (!stockRealTimePriceByCode.getCode().contains("SZ30") && stockRealTimePriceByCode.getIncreaseRate() > 7)) {
                double v = stockRealTimePriceByCode.getCurrentPrice() * 100;
                if (v <= plankConfig.getSingleTransactionLimitAmount() &&
                        AutomaticTrading.todayCostMoney.intValue() + v < plankConfig.getAutomaticTradingMoneyLimitUp()
                        && !PLANK_MONITOR.contains(e)) {
                    PLANK_MONITOR.add(e);
                    if (AutomaticTrading.pendingOrderSet.contains(e)) {
                        PLANK_MONITOR.remove(e);
                    }
                    log.warn("{} 新加入打板监测,当前共监测:{}支股票", e, PLANK_MONITOR.size());
                }
            }
        });
    }

    /**
     * 一直监控主板涨幅大于7个点股票，创业板涨幅大于18个点的股票,即PLANK_MONITOR中的股票
     */
    @Override
    public void run(String... args) {
        filterStock();
        Barbarossa.executorService.submit(this::autoPlank);
    }

    /**
     * 只打上午的板 DateUtil.isAM(new Date());
     * 或者10点以前涨停的板
     */
    private void autoPlank() {
        while (openAutoPlank()) {
            try {
                if (!PLANK_MONITOR.isEmpty()) {
                    for (String code : PLANK_MONITOR) {
                        StockRealTimePrice stockRealTimePriceByCode = stockProcessor.getStockRealTimePriceByCode(code);
                        if (stockRealTimePriceByCode.isPlank()) {
                            Stock stock = stockMapper.selectOne(new QueryWrapper<Stock>().eq("code", stockRealTimePriceByCode.getCode()));
                            if (Objects.isNull(stock.getBuyTime()) || !DateUtils.isSameDay(new Date(), stock.getBuyTime())) {
                                Barbarossa.STOCK_FILTER_MAP.remove(code);
                                PLANK_MONITOR.remove(code);
                                // 上板，下单排队
                                int sum = 0, amount = 1;
                                while (sum <= plankConfig.getSingleTransactionLimitAmount()) {
                                    sum = (int) (amount++ * 100 * stockRealTimePriceByCode.getCurrentPrice());
                                }
                                amount -= 2;
                                if (amount >= 1) {
                                    automaticTrading.buy(stock, amount * 100, stockRealTimePriceByCode.getLimitUp(),
                                            AutomaticTradingEnum.AUTO_PLANK);
                                }
                            }
                        } else if (stockRealTimePriceByCode.getIncreaseRate() < 5) {
                            PLANK_MONITOR.remove(code);
                        }
                    }
                    Thread.sleep(200);
                } else {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.warn("------------------------ 终止打板监测 ------------------------");
        log.warn("今日自动交易花费金额:{}", AutomaticTrading.todayCostMoney.intValue());
    }

    /**
     * 是否开启自动打板
     * 只打plankConfig.getAutomaticPlankTradingTimeLimit()点前封板的票
     *
     * @return boolean
     */
    private boolean openAutoPlank() {
        return AutomaticTrading.isTradeTime() &&
                DateUtil.hour(new Date(), true) < plankConfig.getAutomaticPlankTradingTimeLimit() &&
                AutomaticTrading.todayCostMoney.intValue() < plankConfig.getAutomaticTradingMoneyLimitUp();
    }
}
