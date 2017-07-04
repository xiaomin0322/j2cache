package net.oschina.j2cache.l2;

import java.util.List;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheExpiredListener;
import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.CacheObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
public class CacheL2Channel implements CacheExpiredListener {

	private final static Logger log = LoggerFactory
			.getLogger(CacheL2Channel.class);

	public final static byte LEVEL_2 = 2;

	private String name;
	private final static CacheL2Channel instance = new CacheL2Channel("default");

	/**
	 * 单例方法
	 *
	 * @return
	 */
	public final static CacheL2Channel getInstance() {
		return instance;
	}

	/**
	 * 初始化缓存通道并连接
	 *
	 * @param name
	 * @throws CacheException
	 */
	private CacheL2Channel(String name) throws CacheException {
		this.name = name;
		try {
			CacheManager.initCacheProvider(this);
			log.info("init  CacheL2Channel :" + this.name );
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
		obj.setLevel(LEVEL_2);
		if (region != null && key != null) {
			obj.setValue(CacheManager.get(LEVEL_2, region, key));
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
				// 分几种情况
				// Object obj1 = CacheManager.get(LEVEL_1, region, key);
				// Object obj2 = CacheManager.get(LEVEL_2, region, key);
				// 1. L1 和 L2 都没有
				// 2. L1 有 L2 没有（这种情况不存在，除非是写 L2 的时候失败
				// 3. L1 没有，L2 有
				// 4. L1 和 L2 都有
				CacheManager.set(LEVEL_2, region, key, value);
			}
		}
		// log.info("write data to cache region="+region+",key="+key+",value="+value);
	}

	/**
	 * 删除缓存
	 *
	 * @param region
	 * @param key
	 */
	public void evict(String region, Object key) {
		CacheManager.evict(LEVEL_2, region, key); // 删除二级缓存
	}

	/**
	 * 批量删除缓存
	 *
	 * @param region
	 * @param keys
	 */
	@SuppressWarnings({ "rawtypes" })
	public void batchEvict(String region, List keys) {
		CacheManager.batchEvict(LEVEL_2, region, keys);
	}

	/**
	 * Clear the cache
	 */
	public void clear(String region) throws CacheException {
		CacheManager.clear(LEVEL_2, region);
	}

	@SuppressWarnings("rawtypes")
	public List keys(String region) throws CacheException {
		return CacheManager.keys(LEVEL_2, region);
	}

	/**
	 * 为了保证每个节点缓存的一致，当某个缓存对象因为超时被清除时，应该通知群组其他成员
	 */
	@Override
	public void notifyElementExpired(String region, Object key) {
		log.debug("Cache data expired, region=" + region + ",key=" + key);

	}

	/**
	 * 关闭到通道的连接
	 */
	public void close() {
		CacheManager.shutdown(LEVEL_2);
	}
}
