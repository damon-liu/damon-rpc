package com.damon.rpc.consumerStub;


import com.damon.rpc.consumer.SkuService;
import com.damon.rpc.consumer.UserService;

/**
 * Description:
 *
 * @author damon.liu
 * Date 2023-03-09 14:47
 */
public class TestHeroRpc {

    public static void main(String [] args){

        //第1次远程调用
        SkuService skuService=(SkuService) DamonRpcProxy.create(SkuService.class);
        String respMsg = skuService.findByName("uid");
        System.out.println(respMsg);

        //第2次远程调用
        UserService userService =  (UserService) DamonRpcProxy.create(UserService.class);
        System.out.println(userService.findById());
    }

}
