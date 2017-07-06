package net.oschina.j2cache.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.oschina.j2cache.Cache;
import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.util.SerializationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis 缓存实现
 *
 * @author winterlau
 */
public class RedisClusterCache implements Cache {

    private final static Logger log = LoggerFactory.getLogger(RedisClusterCache.class);
    private String region;

    public RedisClusterCache(String region) {
        this.region = region;
    }

    /**
     * 生成缓存的 key
     *
     * @param key
     * @return
     */
    @SuppressWarnings("rawtypes")
    private String getKeyName(Object key) {

        if (key instanceof Number)
            return region + ":I:" + key;
        else {
            Class keyClass = key.getClass();
            if (String.class.equals(keyClass) || StringBuffer.class.equals(keyClass) || StringBuilder.class.equals(keyClass))
                return region + ":S:" + key;
        }
        return region + ":O:" + key;
    }

    public static void main(String[] args) {
        RedisClusterCache cache = new RedisClusterCache("User");
        System.out.println(cache.getKeyName("Hello"));
        System.out.println(cache.getKeyName(2));
        System.out.println(cache.getKeyName((byte) 2));
        System.out.println(cache.getKeyName(2L));
    }

    @Override
    public Object get(Object key) throws CacheException {
        Object obj = null;
        try {
            if (null == key)
                return null;
            byte[] b = RedisClusterCacheProvider.getResource().get(getKeyName(key).getBytes());
            if (b != null)
                obj = SerializationUtils.deserialize(b);
        } catch (Exception e) {
            log.error("Error occured when get data from L2 cache", e);
            if (e instanceof IOException || e instanceof NullPointerException)
                evict(key);
        } finally {
        }
        return obj;
    }

    @Override
    public void put(Object key, Object value) throws CacheException {
        if (value == null)
            evict(key);
        else {
            try {
            	RedisClusterCacheProvider.getResource().set(getKeyName(key).getBytes(), SerializationUtils.serialize(value));
            } catch (Exception e) {
                throw new CacheException(e);
            } 
        }
    }

    @Override
    public void update(Object key, Object value) throws CacheException {
        put(key, value);
    }

    @Override
    public void evict(Object key) throws CacheException {
        try {
        	RedisClusterCacheProvider.getResource().del(getKeyName(key));
        } catch (Exception e) {
            throw new CacheException(e);
        } 
    }

    /* (non-Javadoc)
     * @see net.oschina.j2cache.Cache#batchRemove(java.util.List)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void evict(List keys) throws CacheException {
        if (keys == null || keys.size() == 0)
            return;
        try {
            String[] okeys = new String[keys.size()];
            for (int i = 0; i < okeys.length; i++) {
                okeys[i] = getKeyName(keys.get(i));
            }
            RedisClusterCacheProvider.getResource().del(okeys);
        } catch (Exception e) {
            throw new CacheException(e);
        } 
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List keys() throws CacheException {
        try {
            List<String> keys = new ArrayList<String>();
            keys.addAll(RedisClusterCacheProvider.keys(region + ":*"));
            for (int i = 0; i < keys.size(); i++) {
                keys.set(i, keys.get(i).substring(region.length() + 3));
            }
            return keys;
        } catch (Exception e) {
            throw new CacheException(e);
        } 
    }

    @Override
    public void clear() throws CacheException {
        try {
            String[] keys = new String[]{};
            keys = RedisClusterCacheProvider.keys(region + ":*").toArray(keys);
            RedisClusterCacheProvider.getResource().del(keys);
        } catch (Exception e) {
            throw new CacheException(e);
        } 
    }

    @Override
    public void destroy() throws CacheException {
        this.clear();
    }

	@Override
	public void expire(Object key, int seconds) {
	        try {
	            if (null == key)
	                return ;
	            RedisClusterCacheProvider.getResource().expire(getKeyName(key).getBytes(), seconds);
	        } catch (Exception e) {
	            log.error("Error occured when get data from L2 cache", e);
	        } 
	}
}
