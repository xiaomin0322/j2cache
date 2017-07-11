package net.oschina.j2cache.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.oschina.j2cache.CacheException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 限流帮助类
 * @author zzm
 *
 */
public class CurrentLimitUtils {

	private final static Logger log = LoggerFactory
			.getLogger(CurrentLimitUtils.class);

	private static final ConcurrentHashMap<String, Semaphore> SEMAPHORE_MAP = new ConcurrentHashMap<String, Semaphore>();

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
		//最大并发数
		public static final int PERMITS = 10;

		//获取锁等待时间 seconds， 0:一直等待 
		public static final int PERMITS_ACQUIRE_TIMEOUT = 0;
		
		//Callable 方法执行超时时间 seconds
		public static final int CALL_TIMEOUT = 5;
		
		
	
	public static <T> T get(String lockName, Callable<T> callable,int permits) {
		return get(lockName, callable, PERMITS, PERMITS_ACQUIRE_TIMEOUT,CALL_TIMEOUT);
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
	public static <T> T get(String lockName, Callable<T> callable,int permits,int permitsAcquireTimeOut,long callTimeout) {
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
			//查询数据库
			Future<T> future = EXECUTOR_SERVICE.submit(callable);
			//查询数据库
			Object object = future.get(callTimeout, TimeUnit.SECONDS);
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
