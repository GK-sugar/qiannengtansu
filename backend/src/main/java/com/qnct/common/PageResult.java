package com.qnct.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应体（兼容前端 el-table / el-pagination）
 */
@Data
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(Page<T> p) {
        PageResult<T> r = new PageResult<>();
        r.total = p.getTotal();
        r.page = p.getCurrent();
        r.size = p.getSize();
        r.records = p.getRecords();
        return r;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }
}