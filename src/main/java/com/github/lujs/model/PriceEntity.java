package com.github.lujs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Lujs
 * @desc TODO
 * @date 2021/11/23 4:38 下午
 */
@AllArgsConstructor
@Data
public class PriceEntity {

    private Long id;

    /**
     * 类型 @枚举 0 商品价格 1 运费
     */
    private int type;

    /**
     * 实际价格
     * 保留2位小数
     */
    private double actualTotal;

}
