package com.github.lujs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Lujs
 * @desc 均摊结果
 * @date 2021/11/23 4:48 下午
 */
@AllArgsConstructor
@Data
public class SplitterEntity {

    private long itemId;

    private long payId;

    private double payNum;

}
