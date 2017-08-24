j2cache
=======

修改了开源中国的红薯的j2cache，全部依赖库都升级为最新版本，修改为maven结构，且去掉了hibernate的支持
注意：fst最新版本需要jdk1.7支持

具体的依赖库版本请参考pom文件。

主要修改内容如下：
========
    2015-09-07
    修改clear批量删除时二级缓存redis未删除的bug，并增加clear的消息广播，清理集群其他机器的一级缓存

========

    1、整个去掉了针对hibernate的支持，主要是由于原版本中依赖的hibernate版本过低，也不会用，所以干脆去掉，减少依赖
    2、FSTSerializer中使用FSTConfiguration的方式进行序列化
    3、JGroup更新版本后配置文件的更新, message对象获取具体消息内容的方法修改由getRawBuffer修改为getBuffer
    4、Jedis更新版本后，所依赖的commons-pool库也得相应更新，主要是创建jedispool时使用的poolconfig配置的相关修改
    5、在EhCacheProvider中创建CacheManager时，修改为单例方法CacheManager.getInstance()

=========
 20170630
 增加单独使用本地缓存和集中式缓存单独使用
 
 单独使用一级缓存
 
 单独使用二级缓存
 
 一级缓存二级缓存组合使用
 
 缓存同步增加redis subscribe
 
 解决热点key，缓存雪崩,缓存穿透问题，缓存限流
  
 增加 支持 spring 整合，支持注解 el表达式
 
 支持redis 2 redis 3
 
待完善功能
动态感知二级缓存宕机，切换到其他二级缓存集群
