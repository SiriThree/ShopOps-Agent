package com.sirithree.shopops.common.api;

import java.util.List;

public class CommonPage<T> {
    private Integer pageNum;
    private Integer pageSize;
    private Long total;
    private List<T> list;

    public static <T> CommonPage<T> of(List<T> list, Integer pageNum, Integer pageSize, Long total) {
        CommonPage<T> page = new CommonPage<>();
        page.setList(list);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setTotal(total);
        return page;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
