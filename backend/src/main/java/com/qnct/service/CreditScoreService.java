package com.qnct.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 绿色金融：F7 企业低碳信誉分（加权） + F8 预授信额度。
 */
@Service
public class CreditScoreService {

    /** F7：信誉分 = 合规得分×0.3 + 数据质量×0.25 + 减排绩效×0.25 + 履约记录×0.2 */
    public int score(int compliance, int dataQuality, int reduction, int fulfillment) {
        double s = compliance * 0.3 + dataQuality * 0.25 + reduction * 0.25 + fulfillment * 0.2;
        return Math.min(100, Math.max(0, (int) Math.round(s)));
    }

    /** F8：预授信 = 年营收基数 × 信誉分/100 × 0.8 */
    public long creditLimit(long annualBase, int score) {
        return Math.round(annualBase * (score / 100.0) * 0.8);
    }

    public Map<String, Object> profile(int compliance, int dataQuality, int reduction, int fulfillment, long annualBase) {
        int s = score(compliance, dataQuality, reduction, fulfillment);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", s);
        m.put("creditLimit", creditLimit(annualBase, s));
        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("合规", compliance);
        radar.put("数据质量", dataQuality);
        radar.put("减排绩效", reduction);
        radar.put("履约", fulfillment);
        m.put("radar", radar);
        return m;
    }
}