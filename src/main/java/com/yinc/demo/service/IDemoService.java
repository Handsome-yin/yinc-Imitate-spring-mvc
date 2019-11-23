package com.yinc.demo.service;

public interface IDemoService {

    String add(String name);

    String query(long id);

    int update(long id,String name);

    String remote(long id);
}
