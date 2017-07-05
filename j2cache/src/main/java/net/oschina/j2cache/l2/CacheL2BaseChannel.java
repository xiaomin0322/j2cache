package net.oschina.j2cache.l2;

import java.util.List;
import java.util.concurrent.Callable;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.l1.CacheL1BaseChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 缓存多播通道
 *
 * @author Winter Lau
 */
public class CacheL2BaseChannel implements ICacheChannel{

    private final static Logger log = LoggerFactory.getLogger(CacheL2BaseChannel.class);

    public final static byte LEVEL_2 = 2;
    public final static byte LEVEL_1 = 1;

    private final static CacheL2BaseChannel instance = new CacheL2BaseChannel();
    
    /**
     * 单例方法
     *
     * @return
     */
    public final static CacheL2BaseChannel getInstance() {
        return instance;
    }

    
    private CacheL1BaseChannel cacheL1Channel;
    
    public CacheL1BaseChannel getCacheL1Channel(){
    	return cacheL1Channel;
    }
    
    public CacheL2BaseChannel setCacheL1Channel(CacheL1BaseChannel cacheL1Channel){
    	this.cacheL1Channel=cacheL1Channel;
    	return this;
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
            obj.setValue(getCacheL1Channel().get(region, key).getValue());
            if (obj.getValue() == null) {
                obj.setValue(CacheManager.get(LEVEL_2, region, key));
                if (obj.getValue() != null) {
                    obj.setLevel(LEVEL_2);
                    getCacheL1Channel().set(region, key, obj.getValue());
                }
            } else
                obj.setLevel(LEVEL_1);
        }
        return obj;
    }
    
    @Override
	public CacheObject get(String region, Object key, Callable<?> callable) {
    	CacheObject cacheObject = get(region, key);
    	if(cacheObject.getValue() == null){
    		cacheObject = getCacheL1Channel().get(region, key, callable);
    		if(cacheObject.getValue() == null){
    			 CacheManager.set(LEVEL_2, region, key, cacheObject.getValue());
    		}
    	}
		return cacheObject;
	}
    

    /**
     * 写入缓存
     *
     * @param region
     * @param key
     * @param value
     */
    public void set(String region, Object key, Object value) {
    	set(region, key, value, false);
    }

    /**
     * 删除缓存
     *
     * @param region
     * @param key
     */
    public void evict(String region, Object key) {
    	getCacheL1Channel().evict(region, key); //删除一级缓存
        CacheManager.evict(LEVEL_2, region, key); //删除二级缓存
    }

    /**
     * 批量删除缓存
     *
     * @param region
     * @param keys
     */
    @SuppressWarnings({"rawtypes"})
    public void batchEvict(String region, List keys) {
    	getCacheL1Channel().batchEvict(region, keys);
        CacheManager.batchEvict(LEVEL_2, region, keys);
    }

    /**
     * Clear the cache
     */
    public void clear(String region) throws CacheException {
    	getCacheL1Channel().clear(region);
        CacheManager.clear(LEVEL_2, region);
    }



    /**
     * 为了保证每个节点缓存的一致，当某个缓存对象因为超时被清除时，应该通知群组其他成员
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void notifyElementExpired(String region, Object key) {
        log.debug("Cache data expired, region=" + region + ",key=" + key);
        //删除二级缓存
        if (key instanceof List)
            CacheManager.batchEvict(LEVEL_2, region, (List) key);
        else
            CacheManager.evict(LEVEL_2, region, key);
        //发送广播
        _sendEvictCmd(region, key);
    }


    /**
     * 关闭到通道的连接
     */
    public void close() {
    	getCacheL1Channel().close();
        CacheManager.shutdown(LEVEL_2);
    }

	@Override
	public void set(String region, Object key, Object value, boolean sysCluster) {
		 getCacheL1Channel().set(region, key, value, sysCluster);
		 CacheManager.set(LEVEL_2, region, key, value);
		 if(!sysCluster){
			 _sendEvictCmd(region, key);
		 }
	     log.info("write data to cache region="+region+",key="+key+",value="+value);
	}

	@Override
	public void _sendClearCmd(String region) {
		getCacheL1Channel()._sendClearCmd(region);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List keys(String region) throws CacheException {
		return getCacheL1Channel().keys(region);
	}

	@Override
	public void _sendEvictCmd(String region, Object key) {
		getCacheL1Channel()._sendEvictCmd(region, key);
	}

	@Override
	public void onDeleteCacheKey(String region, Object key) {
		getCacheL1Channel().onDeleteCacheKey(region, key);
		
	}

	@Override
	public void onClearCache(String region) {
		getCacheL1Channel().onClearCache(region);
	}

	


}
