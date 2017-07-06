package net.oschina.j2cache.l1;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.CacheOprator;
import net.oschina.j2cache.Command;
import net.oschina.j2cache.redis.RedisCacheProvider;
import net.oschina.j2cache.redis.RedisClusterCacheProvider;
import net.oschina.j2cache.util.CacheUtils;
import net.oschina.j2cache.util.SerializationUtils;
import net.oschina.j2cache.util.SpringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
public class CacheL1RedisChannel extends CacheL1BaseChannel {

    private final static Logger log = LoggerFactory.getLogger(CacheL1RedisChannel.class);

    private String name;
    
    private final static CacheL1RedisChannel instance = new CacheL1RedisChannel("default");
    
    
    /**
     * 单例方法
     *
     * @return
     */
    public final static CacheL1RedisChannel getInstance() {
        return instance;
    }

    /**
     * 初始化缓存通道并连接
     *
     * @param name
     * @throws CacheException
     */
    private CacheL1RedisChannel(String name) throws CacheException {
        this.name = name;
        try {
        	 long ct = System.currentTimeMillis();
        	 
            CacheManager.initCacheProvider(this);
            String channelKey = "cache.L1.provider.redis.subscribe.keys";
            String channelStr = SpringProperty.getProperty(channelKey);
            if(StringUtils.isEmpty(channelStr)){
            	channelStr = CacheManager.getProperties().getProperty(channelKey);
            }
            log.info("redis subscribe : {}",channelStr);
            for(final String key:channelStr.split(",")){
            	if(StringUtils.isEmpty(key)){
            		continue;
            	}
            	String l2_provider_class = CacheManager.getProperties().getProperty("cache.L2.provider_class");
            	 new Thread(){
                 	public void run() {
                 		if("redis".equalsIgnoreCase(l2_provider_class)){
                 			RedisCacheProvider.getResource().subscribe(new  RedisMQBinaryHandler(), getChannelNameByte(key));
                 		}else{
                 			RedisClusterCacheProvider.getResource().subscribe(new  RedisMQBinaryHandler(), getChannelNameByte(key));
                 		}
                 	}
                 }.start();
            }
           log.info("CacheL1RedisChannel init :" + this.name + ", time " + (System.currentTimeMillis() - ct) + " ms.");
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

      public void _sendClearCmd(String region) {
        
        Jedis cache = RedisCacheProvider.getResource();
        try {
        	// 发送广播
            Command cmd = new Command(CacheOprator.REMOVE_ALL, region, "");
        	cache.publish(getChannelNameByte(region), SerializationUtils.serialize(cmd));
        } catch (Exception e) {
            log.error("Unable to clear cache,region=" + region, e);
        }finally{
        	RedisCacheProvider.returnResource(cache, false);
        }
    }
    
    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendSetCmd(String region, Object key,Object val) {
    	Jedis cache = RedisCacheProvider.getResource();
        try {
             //发送广播
             Command cmd = new Command(CacheOprator.SET, region, key,val);
        	 cache.publish(getChannelNameByte(region), SerializationUtils.serialize(cmd));
        } catch (Exception e) {
            log.error("Unable to delete cache,region=" + region + ",key=" + key, e);
        }finally{
        	RedisCacheProvider.returnResource(cache, false);
        }
    }

    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendEvictCmd(String region, Object key) {
    	Jedis cache = RedisCacheProvider.getResource();
        try {
             //发送广播
             Command cmd = new Command(CacheOprator.REMOVE, region, key);
        	 cache.publish(getChannelNameByte(region), SerializationUtils.serialize(cmd));
        } catch (Exception e) {
            log.error("Unable to delete cache,region=" + region + ",key=" + key, e);
        }finally{
        	RedisCacheProvider.returnResource(cache, false);
        }
    }



    private static class RedisMQBinaryHandler extends BinaryJedisPubSub {  
	       @Override
	    public void onMessage(byte[] channel, byte[] message) {
	    	   CacheUtils.onMessage(message); 
	    }
   }     
    
}
