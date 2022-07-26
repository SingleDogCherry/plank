# plank 涨停先锋

#### Introduction

- 抓取数据：A股每支股票每日的成交数据，涨跌幅度，最高最低价等等，每日的龙虎榜数据
- 监控数据：监控自己的持仓，重点关注的股票，主力实时流入
- 选股策略：找出爆量回踩的票（我一般青睐这个模式），找出最近走上升趋势的股票等
- 晋级胜率：找出最近的连板股梯队，分析出连板股的晋级胜率，为打板和干龙头提供依据
- 模拟操盘：以历史数据为样本，配置好买入，卖出，分仓，减仓，清仓策略，以此来回测不同选股、买入、卖出策略的收益率（效果并不理想，选股，买卖策略还不够完善）

> 我最近走趋势流了，看图形和版块买，跟随资金，有炒大A的朋友可以加V(GODR3060W)交流.

> 运行之前研究一下配置文件里面的参数，特别是雪球的cookie需要隔断时间换一下
>
> 买入标的股的选择是根据前一天龙虎榜的净流入大小排名来决定（简单粗暴）。 因为机器不可能像我们人盯盘那样感知盘面情绪的变化。华鑫上分量化打板YYDS，它的模型和策略是做的最好的。

连板晋级率：

![avatar](./src/main/resources/img/1.png)

> 连板越高，晋级概率越大。三板定龙头是有道理的。当然，有可能很多都是一字板，排单都排不进去。
>

以历史样本数据自动模拟操盘：

![avatar](./src/main/resources/img/2.png)

监控数据：

![avatar](./src/main/resources/img/3.png)

基金和外资持仓，季度增减仓数据： sql:src/main/resources/query.text
![avatar](./src/main/resources/img/4.png)