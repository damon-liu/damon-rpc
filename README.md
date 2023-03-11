## 一、项目介绍

damon-rpc项目是基于neety+jdk动态代理实现简易rpc框架。

首先了解一下neety的通信模型：

![image-20230311231407455](https://damon-study.oss-cn-shenzhen.aliyuncs.com/%20typora/%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8Bimage-20230311231407455.png)

本项目的模型图，重点在03与06处

![image-20230311231657868](https://damon-study.oss-cn-shenzhen.aliyuncs.com/%20typora/%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8Bimage-20230311231657868.png)

## 二、测试

先启动damon-producer提供者服务，再直接运行damon-consumer消费服务中的TestHeroRpc.main方法

```
public static void main(String [] args){

        //第1次远程调用
        SkuService skuService=(SkuService) DamonRpcProxy.create(SkuService.class);
        String respMsg = skuService.findByName("uid");
        System.out.println(respMsg);

        //第2次远程调用
        UserService userService =  (UserService) DamonRpcProxy.create(UserService.class);
        System.out.println(userService.findById());
    }
```

结果：

![image-20230311232159616](https://damon-study.oss-cn-shenzhen.aliyuncs.com/%20typora/%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8Bimage-20230311232159616.png)

## 三、核心部分

### 3.1 damon-producer

项目启动时DamonRpcServer.init()方法会被加载，用于监听指定端口是否有时间发生。

```java
 @PostConstruct
    public void init() {
        new Thread(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .localAddress(nettyPort)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                //编码器
                                pipeline.addLast("encoder", new ObjectEncoder());
                                //解码器
                                pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                                //服务器端业务处理类
                                pipeline.addLast(new InvokeHandler());
                            }
                        });
                ChannelFuture future = serverBootstrap.bind(nettyPort).sync();
                System.out.println("......Hero RPC is ready......");
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).start();
    }
```

InvokeHandler.channelRead()方法是neety读取到客户端发来的数据并通过反射调用实现类的方法

```java
@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ClassInfo classInfo = (ClassInfo) msg;
        Object clazz = Class.forName(getImplClassName(classInfo)).newInstance();
        Method method = clazz.getClass().getMethod(classInfo.getMethodName(), classInfo.getTypes());
        //通过反射调用实现类的方法
        Object result = method.invoke(clazz, classInfo.getObjects());
        ctx.writeAndFlush(result);
    }
```

### 3.2 damon-consumer

DamonRpcProxy.create()方法。

基于jdk的动态代理实现了在运行期对调用的目标方法的增强，主要体现在植入了netty客户端代码，实现了consumer像调用本地接口一样调用远程服务接口。

```
public static Object create(Class target) {

        ClassLoader classLoader = target.getClassLoader();
        Class[] interfaces = {target};

        InvocationHandler invocation = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                //封装ClassInfo
                ClassInfo classInfo = new ClassInfo();
                classInfo.setClassName(target.getName());
                classInfo.setMethodName(method.getName());
                classInfo.setObjects(args);
                classInfo.setTypes(method.getParameterTypes());

                //开始用Netty发送数据
                EventLoopGroup group = new NioEventLoopGroup();
                ResultHandler resultHandler = new ResultHandler();
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    ChannelPipeline pipeline = ch.pipeline();
                                    //编码器
                                    pipeline.addLast("encoder", new ObjectEncoder());
                                    //解码器  构造方法第一个参数设置二进制数据的最大字节数  第二个参数设置具体使用哪个类解析器
                                    pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                                    //客户端业务处理类
                                    pipeline.addLast("handler", resultHandler);
                                }
                            });
                    ChannelFuture future = b.connect("127.0.0.1", 9999).sync();
                    future.channel().writeAndFlush(classInfo).sync();
                    future.channel().closeFuture().sync();
                } finally {
                    group.shutdownGracefully();
                }
                return resultHandler.getResponse();
            }
        };
        //创建一个代理对象
        return Proxy.newProxyInstance(classLoader, interfaces, invocation);
    }
```

## 写在最后

由于本人目前在准备换工作，无暇编写更详细的文档，请小伙伴们先自行阅读核心部分的代码，若对项目有更好建议者，请发送邮件至670682988@qq.com，如果觉得本项目比较nice,麻烦动动您发财的小手**start**一下，感谢！

