package com.github.lujs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Lujs
 * @desc 支付实体
 * @date 2021/11/23 4:43 下午
 */
@AllArgsConstructor
@Data
public class PayEntity {

    private long id;

    /**
     * 支付类型
     * @枚举 0 三方支付 1 余额 2 卡券
     */
    private int type;

    /**
     * 支付金额
     * 保留2位小数
     */
    private double num;

}
