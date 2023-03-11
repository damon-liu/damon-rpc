package com.damon.rpc.producer.impl;


import com.damon.rpc.producer.UserService;

public class UserServiceImpl implements UserService {
    @Override
    public String findById() {
        return "user{id=1,username=xiongge}";
    }
}
