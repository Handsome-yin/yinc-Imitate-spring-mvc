package com.yinc.demo.service.impl;

import com.yinc.demo.service.IDemoService;
import com.yinc.demo.service.Test2;
import com.yinc.mvcframework.annotation.Service;

@Service
public class IDemoServiceImpl implements IDemoService, Test2 {
    @Override
    public String add(String name) {
        return "添加成功,我是：" + name;
    }

    @Override
    public String query(long id) {
        return "我的ID是：" + id;
    }

    @Override
    public int update(long id, String name) {
        return 1;
    }

    @Override
    public String remote(long id) {
        return "id为：" + id + "删除成功。";
    }
}
