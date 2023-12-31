ALTER TABLE `plank`.`trade_record`
DROP COLUMN `balance`,
DROP COLUMN `available_balance`,
MODIFY COLUMN `price` decimal(10, 2) NOT NULL COMMENT '卖出价格' AFTER `number`,
ADD COLUMN `profit` decimal(10, 2) NOT NULL COMMENT '本笔交易利润' AFTER `reason`,
ADD COLUMN `cost_price` decimal(10, 2) NOT NULL COMMENT '成本价' AFTER `profit`;


ALTER TABLE `plank`.`hold_shares`
    ADD COLUMN `highest_profit_ratio` decimal(10, 2) NULL COMMENT '最高盈利百分比' AFTER `sale_time`;

ALTER TABLE `plank`.`hold_shares`
    ADD COLUMN `clearance_reason` varchar(32) NULL DEFAULT 0 COMMENT '清仓原因' AFTER `clearance`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;