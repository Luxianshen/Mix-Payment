package com.github.lujs.method;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.lujs.model.PayEntity;
import com.github.lujs.model.PriceEntity;
import com.github.lujs.model.SplitterEntity;
import lombok.extern.log4j.Log4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Lujs
 * @desc 价格均摊工具类
 * @date 2021/10/26 10:15 上午
 */
@Log4j
public class OrderSplitter {

    private static boolean balanceFlag = false;

    private static boolean thirdFlag = false;

    /**
     * 根据订单价格比例拆分
     *
     * @param priceEntityList  订单价格项
     * @param payEntityList  支付项
     * @param checkFlag 是否 价值校验
     */
    public static List<SplitterEntity> proportion(List<PriceEntity> priceEntityList, List<PayEntity> payEntityList, boolean checkFlag) {
        return doProportion(priceEntityList, payEntityList, checkFlag);
    }

    private static List<SplitterEntity> doProportion(List<PriceEntity> priceEntityList, List<PayEntity> payEntityList, boolean checkFlag) {

        //分母
        double sum = round2(payEntityList.stream().mapToDouble(PayEntity::getNum).sum());

        final List<PayEntity> sortPayList = payEntityList.stream().sorted(Comparator.comparingDouble(PayEntity::getNum)).collect(Collectors.toList());

        //价格排序
        priceEntityList = priceEntityList.stream().sorted(Comparator.comparingDouble(PriceEntity::getActualTotal)).collect(Collectors.toList());

        CopyOnWriteArrayList<SplitterEntity> dataList = new CopyOnWriteArrayList<>();

        //游标
        AtomicInteger countSum = new AtomicInteger(0);

        int runFlag = priceEntityList.size();

        priceEntityList.forEach(x -> {
            //判断最后一次  ++i
            if (countSum.addAndGet(1) == runFlag) {
                sortPayList.forEach(y -> {
                    double tempDouble = round2(y.getNum() - dataList.stream().filter(z -> z.getPayId() == y.getId()).mapToDouble(SplitterEntity::getPayNum).sum());
                    dataList.add(new SplitterEntity(x.getId(), y.getId(), tempDouble));
                });
            } else {
                sortPayList.forEach(y -> {
                    double tempDouble = round2(y.getNum() * x.getActualTotal() / sum);
                    dataList.add(new SplitterEntity(x.getId(), y.getId(), tempDouble));
                });
            }
        });

        //价格校验
        if (checkFlag) {
            priceEntityList.forEach(x -> {
                List<SplitterEntity> itemList = dataList.stream().filter(y -> y.getItemId() == x.getId()).collect(Collectors.toList());
                double itemSum = round2(itemList.stream().mapToDouble(SplitterEntity::getPayNum).sum());
                if (itemSum != x.getActualTotal()) {
                    //找到最大项 更新
                    double need = round2(itemSum - x.getActualTotal());
                    SplitterEntity splitterEntity = itemList.get(itemList.size() - 1);
                    //找出对应的pay 整项调整
                    dataList.remove(splitterEntity);
                    splitterEntity.setPayNum(splitterEntity.getPayNum() - need);
                    dataList.add(splitterEntity);
                }
            });
        }

        return dataList;
    }

    private static double round2(double v) {
        return NumberUtil.round(v, 2).doubleValue();
    }


    /**
     * 数据产生
     *
     * @param priceList
     * @param payList
     */
    private static void dataMaker(List<PriceEntity> priceList, List<PayEntity> payList) {

        //随机项数
        int itemNum = RandomUtil.randomInt(1, 10);
        //随机支付数
        int payNum = RandomUtil.randomInt(1, 6);

        for (long i = 0; i < itemNum; i++) {
            priceList.add(new PriceEntity(i, 0, round2(RandomUtil.randomDouble(0.01, 100))));
        }
        System.out.println("商品项数：" + priceList.size() +
                " 商品总价：" + round2(priceList.stream().mapToDouble(PriceEntity::getActualTotal).sum()));

        //商品总数 余额 和三方 只能出现一次 卡随意次数
        double priceTotal = round2(priceList.stream().mapToDouble(PriceEntity::getActualTotal).sum());

        balanceFlag = false;
        thirdFlag = false;

        for (long i = 0; i < payNum - 1; i++) {
            int payType = getPayType();
            double itemPrice = 0.01;
            double max = round2(priceTotal - (i + 1) * 0.01);
            if (max > itemPrice) {
                itemPrice = round2(RandomUtil.randomDouble(0.01, max));
            }
            priceTotal = round2(priceTotal - itemPrice);
            payList.add(new PayEntity(i, payType, itemPrice));
        }
        payList.add(new PayEntity(new Long(payNum - 1), getPayType(), round2(priceTotal)));

        System.out.println("支付项数：" + payList.size() +
                " 支付总数：" + round2(payList.stream().mapToDouble(PayEntity::getNum).sum()));

    }

    private static int getPayType() {
        int type = RandomUtil.randomInt(0, 2);
        if (type == 0 && !balanceFlag) {
            balanceFlag = true;
        } else if (type == 1 && !thirdFlag) {
            thirdFlag = true;
        } else {
            type = 2;
        }
        return type;
    }

    private static int dataCheck(List<PriceEntity> priceList, List<PayEntity> payList, List<SplitterEntity> proportion) {
        AtomicInteger falseNum = new AtomicInteger();
        //订单单项校验
        priceList.forEach(x -> {
            double splitterSum = round2(proportion.stream().filter(y -> y.getItemId() == x.getId()).mapToDouble(SplitterEntity::getPayNum).sum());
            if (splitterSum != x.getActualTotal()) {
                System.out.println("均分后的价格：" + splitterSum + " 单项价格" + x.getActualTotal());
                falseNum.getAndAdd(1);
            }
        });
        //支付校验
        payList.forEach(x -> {
            double splitterSum = round2(proportion.stream().filter(y -> y.getPayId() == x.getId()).mapToDouble(SplitterEntity::getPayNum).sum());
            if (splitterSum != x.getNum()) {
                System.out.println("均分后的价格：" + splitterSum + " 单项价格" + x.getNum());
                falseNum.getAndAdd(1);
            }
        });
        return falseNum.get();
    }


    /**
     * 普通拆分
     */
    public static void main(String[] args) {

        List<PriceEntity> priceList;
        List<PayEntity> payList;
        int runTime = RandomUtil.randomInt(100000, 1000000);
        for (int i = 0; i < runTime; i++) {
            priceList = new ArrayList<>();
            payList = new ArrayList<>();
            dataMaker(priceList, payList);
            List<SplitterEntity> proportion = proportion(priceList, payList, true);
            Assert.isTrue(dataCheck(priceList, payList, proportion) == 0);
        }
    }


}

