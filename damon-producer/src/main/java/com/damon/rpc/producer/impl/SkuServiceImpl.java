package com.damon.rpc.producer.impl;


import com.damon.rpc.producer.SkuService;

public class SkuServiceImpl implements SkuService {
    @Override
    public String findByName(String name) {
        return "sku{}:" + name;
    }
}
