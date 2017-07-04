package net.oschina.j2cache;

import java.util.List;

import net.oschina.j2cache.redis.RedisCacheProvider;
import net.oschina.j2cache.util.SerializationUtils;

import org.jgroups.Address;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
public class CacheL1RedisChannel implements ICacheChannel,CacheExpiredListener {

    private final static Logger log = LoggerFactory.getLogger(CacheL1RedisChannel.class);

    private String name;
    
    public String channel = "cmallRedisCacheChannel";
    
    public byte[] channelByte = null;

    private final static CacheL1RedisChannel instance = new CacheL1RedisChannel("default");

    public final static byte LEVEL_1 = 1;
    
    
    
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
        	channelByte =  SerializationUtils.serialize(channel);
            CacheManager.initCacheProvider(this);
            long ct = System.currentTimeMillis();
            new Thread(){
            	public void run() {
            		 Jedis cache = RedisCacheProvider.getResource();
            		 cache.subscribe(new  RedisMQBinaryHandler(), channelByte);
            		 RedisCacheProvider.returnResource(cache, false);
            	}
            }.start();
            log.info("Connected to channel:" + this.name + ", time " + (System.currentTimeMillis() - ct) + " ms.");
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * 获取缓存中的数据
     *
     * @param region
     * @param key
     * @return
     */
    public CacheObject get(String region, Object key) {
        CacheObject obj = new CacheObject();
        obj.setRegion(region);
        obj.setKey(key);
        if (region != null && key != null) {
            obj.setValue(CacheManager.get(LEVEL_1, region, key));
            if (obj.getValue() == null) {
            } else
                obj.setLevel(LEVEL_1);
        }
        return obj;
    }

    /**
     * 写入缓存
     *
     * @param region
     * @param key
     * @param value
     */
    public void set(String region, Object key, Object value) {
        if (region != null && key != null) {
            if (value == null)
                evict(region, key);
            else {
                //分几种情况
                //Object obj1 = CacheManager.get(LEVEL_1, region, key);
                //Object obj2 = CacheManager.get(LEVEL_2, region, key);
                //1. L1 和 L2 都没有
                //2. L1 有 L2 没有（这种情况不存在，除非是写 L2 的时候失败
                //3. L1 没有，L2 有
                //4. L1 和 L2 都有
                _sendEvictCmd(region, key);//清除原有的一级缓存的内容
                CacheManager.set(LEVEL_1, region, key, value);
            }
        }
        log.info("write data to cache region="+region+",key="+key+",value="+value);
    }

    /**
     * 删除缓存
     *
     * @param region
     * @param key
     */
    public void evict(String region, Object key) {
        CacheManager.evict(LEVEL_1, region, key); //删除一级缓存
        _sendEvictCmd(region, key); //发送广播
    }

    /**
     * 批量删除缓存
     *
     * @param region
     * @param keys
     */
    @SuppressWarnings({"rawtypes"})
    public void batchEvict(String region, List keys) {
        CacheManager.batchEvict(LEVEL_1, region, keys);
        _sendEvictCmd(region, keys);
    }

    /**
     * Clear the cache
     */
    public void clear(String region) throws CacheException {
        CacheManager.clear(LEVEL_1, region);
        _sendClearCmd(region);
    }

   

    @SuppressWarnings("rawtypes")
    public List keys(String region) throws CacheException {
        return CacheManager.keys(LEVEL_1, region);
    }

    /**
     * 为了保证每个节点缓存的一致，当某个缓存对象因为超时被清除时，应该通知群组其他成员
     */
    @Override
    public void notifyElementExpired(String region, Object key) {
        log.debug("Cache data expired, region=" + region + ",key=" + key);
        //发送广播
        _sendEvictCmd(region, key);
    }
    
    public void _sendClearCmd(String region) {
        // 发送广播
        Command cmd = new Command(CacheOprator.REMOVE_ALL, region, "");
        try {
        	 Jedis cache = RedisCacheProvider.getResource();
        	  cache.publish(channelByte, SerializationUtils.serialize(cmd));
        	 RedisCacheProvider.returnResource(cache, false);
        } catch (Exception e) {
            log.error("Unable to clear cache,region=" + region, e);
        }
    }

    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendEvictCmd(String region, Object key) {
        //发送广播
        Command cmd = new Command(CacheOprator.REMOVE, region, key);
        try {
        	 Jedis cache = RedisCacheProvider.getResource();
        	 cache.publish(channelByte, SerializationUtils.serialize(cmd));
        	 RedisCacheProvider.returnResource(cache, false);
        } catch (Exception e) {
            log.error("Unable to delete cache,region=" + region + ",key=" + key, e);
        }
    }

    /**
     * 删除一级缓存的键对应内容
     *
     * @param region
     * @param key
     */
    @SuppressWarnings("rawtypes")
    public void onDeleteCacheKey(String region, Object key) {
        if (key instanceof List)
            CacheManager.batchEvict(LEVEL_1, region, (List) key);
        else
            CacheManager.evict(LEVEL_1, region, key);
        log.debug("Received cache evict message, region=" + region + ",key=" + key);
    }

  

    public void onClearCache(String region) {
        CacheManager.clear(LEVEL_1, region);
    }

    /**
     * 组中成员变化时
     */
    public void viewAccepted(View view) {
        StringBuffer sb = new StringBuffer("Group Members Changed, LIST: ");
        List<Address> addrs = view.getMembers();
        for (int i = 0; i < addrs.size(); i++) {
            if (i > 0)
                sb.append(',');
            sb.append(addrs.get(i).toString());
        }
        log.info(sb.toString());
    }

    /**
     * 关闭到通道的连接
     */
    public void close() {
        CacheManager.shutdown(LEVEL_1);
    }

    private static class RedisMQBinaryHandler extends BinaryJedisPubSub {  
	       @Override
	    public void onMessage(byte[] channel, byte[] message) {
	    	 try {
	    		Command cmd = (Command) SerializationUtils.deserialize(message);
				//不处理发送给自己的消息
		        if (cmd.getMsgId().equalsIgnoreCase(Command.ID)){
		        	  return;
		        }
		        CacheL1RedisChannel cacheL1RedisChannel = CacheL1RedisChannel.getInstance();
		        System.out.println("channel="+SerializationUtils.deserialize(channel)+" message="+cmd);
		        
		        switch (cmd.getOperator()) {
		                case REMOVE:
		                	cacheL1RedisChannel.onDeleteCacheKey(cmd.getRegion(), cmd.getKey());
		                    break;
		                case REMOVE_ALL:
		                	cacheL1RedisChannel.onClearCache(cmd.getRegion());
		                    break;
		                default:
		                    log.warn("Unknown message type = " + cmd.getOperator());
		            }
		        } catch (Exception e) {
		            log.error("Unable to handle received msg", e);
		       } 
	    }
}     
    
}
