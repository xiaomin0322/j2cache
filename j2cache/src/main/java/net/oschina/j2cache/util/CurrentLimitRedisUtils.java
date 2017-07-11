package net.oschina.j2cache.util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.redis.RedisClusterCacheProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 限流帮助类
 * @author zzm
 *
 */
public class CurrentLimitRedisUtils {

	private final static Logger log = LoggerFactory
			.getLogger(CurrentLimitRedisUtils.class);

	private static final ConcurrentHashMap<String, Semaphore> SEMAPHORE_MAP = new ConcurrentHashMap<String, Semaphore>();
	
	
	
   private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
	
	//空缓存名称
	public static final String CACHE_VAL = "NULL";
	
	//最大并发数
	public static final int PERMITS = 10;

	//获取锁等待时间 seconds， 0:一直等待 
	public static final int PERMITS_ACQUIRE_TIMEOUT = 0;
	
	//liveTime 空缓存失效时间 seconds
	public static final int CACHE_LIVE_TIME = 60;
	
	//Callable 方法执行超时时间 seconds
	public static final int CALL_TIMEOUT = 5;
	

	
	public static <T> T get(String lockName,String cacheKey,Callable<T> callable,int permits) {
		return get(lockName,cacheKey,callable, PERMITS, PERMITS_ACQUIRE_TIMEOUT,CACHE_LIVE_TIME,CALL_TIMEOUT);
	}
	/**
	 * 获取缓存中的数据
	 *
	 * @param key
	 * @param callable
	 * @param permits 最大并发数
	 * @param permitsAcquireTimeOut 获取锁等待时间
	 * @return obj
	 */
	@SuppressWarnings("unchecked")
	public  static <T> T get(String lockName,String cacheKey, Callable<T> callable,int permits,int permitsAcquireTimeOut,long liveTime,long callTimeout){
		Object object = null;
		byte[] cacheKeyBytes  = null;
		try {
			cacheKeyBytes = SerializationUtils.serialize(cacheKey);
		} catch (IOException e1) {
			log.error("get IOException", e1);
		}
		byte[] bytes = RedisClusterCacheProvider.getResource().get(cacheKeyBytes);
		try {
			object = SerializationUtils.deserialize(bytes);
		} catch (IOException e1) {
			log.error("get IOException", e1);
		}
		
		if(object !=null){
			if(CACHE_VAL.equals(object)){
				return null;
			}else{
				return (T) object;
			}
		}
		
		
		boolean tryAcquireFlag = true;
		Semaphore semaphore = SEMAPHORE_MAP.get(lockName);
		if (semaphore == null) {
			semaphore = new Semaphore(permits, true);
			Semaphore l = SEMAPHORE_MAP.putIfAbsent(lockName, semaphore);
			if (l != null) {
				semaphore = l;
			}
		}
		try {
			if (permitsAcquireTimeOut <= 0) {
				semaphore.acquire();
				log.info("semaphore hashCode {} semaphore.acquire {} lockName",
						semaphore.hashCode(), lockName);
			} else {
				tryAcquireFlag = semaphore.tryAcquire(permitsAcquireTimeOut,
						TimeUnit.MILLISECONDS);
				log.info(
						"semaphore hashCode {} semaphore.tryAcquire lockName {}  time:{} SECONDS flag :{}",
						semaphore.hashCode(), lockName, permitsAcquireTimeOut,
						tryAcquireFlag);
				if (!tryAcquireFlag) {
					throw new CacheException("semaphore.tryAcquire false");
				}
			}
			
			bytes = RedisClusterCacheProvider.getResource().get(cacheKeyBytes);
			object = SerializationUtils.deserialize(bytes);
			if(object !=null){
				if(CACHE_VAL.equals(object)){
					return null;
				}else{
					return (T) object;
				}
			}
			//查询数据库
			Future<T> future = EXECUTOR_SERVICE.submit(callable);
			//查询数据库
			object = future.get(callTimeout, TimeUnit.SECONDS);
			
			object = object == null ? CACHE_VAL : object;
			
			//加入缓存
			RedisClusterCacheProvider.getResource().set(cacheKeyBytes, SerializationUtils.serialize(object));
			//callable为null则，缓存空结果，防止缓存穿透
			if(CACHE_VAL.equals(object)){
				RedisClusterCacheProvider.getResource().expire(cacheKeyBytes, 60);
				object = null;
			}
			return (T) object;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (semaphore != null && tryAcquireFlag) {
				semaphore.release();
				log.info("semaphore.release {} lockName", lockName);
			}
		}
	}
}
