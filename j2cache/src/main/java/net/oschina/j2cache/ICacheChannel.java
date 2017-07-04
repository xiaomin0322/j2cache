package net.oschina.j2cache;

import java.util.List;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
@SuppressWarnings("all")
public interface ICacheChannel extends CacheExpiredListener{

    /**
     * 获取缓存中的数据
     *
     * @param region
     * @param key
     * @return
     */
    public CacheObject get(String region, Object key);

    /**
     * 写入缓存
     *
     * @param region
     * @param key
     * @param value
     */
    public void set(String region, Object key, Object value);
    
    /**
     * 写入缓存
     *
     * @param region
     * @param key
     * @param value
     */
    public void set(String region, Object key, Object value,boolean sysCluster);
    
    /**
     * 删除缓存
     *
     * @param region
     * @param key
     */
    public void evict(String region, Object key);

    /**
     * 批量删除缓存
     *
     * @param region
     * @param keys
     */
    public void batchEvict(String region, List keys);

    /**
     * Clear the cache
     */
    public void clear(String region) throws CacheException;
    
    
    public void _sendClearCmd(String region);

   
	public List keys(String region) throws CacheException;
    
    /**
     * 为了保证每个节点缓存的一致，当某个缓存对象因为超时被清除时，应该通知群组其他成员
     */
    public void notifyElementExpired(String region, Object key);

    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendEvictCmd(String region, Object key);
    
    /**
     * 删除一级缓存的键对应内容
     *
     * @param region
     * @param key
     */
    public void onDeleteCacheKey(String region, Object key);

  

    public void onClearCache(String region);
    
    
    public void close();



}
