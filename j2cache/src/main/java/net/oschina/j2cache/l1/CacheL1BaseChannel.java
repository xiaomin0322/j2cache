package net.oschina.j2cache.l1;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.util.SerializationUtils;

import org.jgroups.Address;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
public abstract class CacheL1BaseChannel  implements ICacheChannel{

    private final static Logger log = LoggerFactory.getLogger(CacheL1BaseChannel.class);
    
	private static final ConcurrentHashMap<String,Semaphore> SEMAPHORE_MAP = new ConcurrentHashMap<String, Semaphore>();

    public final static byte LEVEL_1 = 1;//一级缓存
    public final static byte LEVEL_3 = 3;//call操作
    public final static String NULL = "NULL";
    
    public static String getChannelName(String name){
    	return "cache_"+name;
    }
    
    public static byte[] getChannelNameByte(String name){
    	try {
			return SerializationUtils.serialize(getChannelName(name));
		} catch (IOException e) {
			 log.error("SerializationUtils.serialize IOException", e);
		}
    	return null;
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
            if(obj.getValue()!=null){
            	obj.setLevel(LEVEL_1);
            }
        }
        return obj;
    }
    
    /**
     * 获取缓存中的数据
     *
     * @param region
     * @param key
     * @param callable
     * @return
     */
    public CacheObject get(String region, Object key,Callable<?> callable){
    	CacheObject cacheObject = get(region, key);
    	if(cacheObject.getValue() == null){
    		//获取线程许可数
    		int permits =Integer.parseInt(CacheManager.getProperties().getProperty("cache.L1.provider_permits", "5"));
    		log.info("permits {} ",permits);
    		//获取锁的最大等待时间
    		long permitsAcquireTimeOut = Integer.parseInt(CacheManager.getProperties().getProperty("cache.L1.provider_permitsAcquireTimeOut", "0"));;
    		log.info("permitsAcquireTimeOut {} ",permitsAcquireTimeOut);
    		
    		
    		boolean tryAcquireFlag = true;
    		Semaphore semaphore = SEMAPHORE_MAP.get(region);
		    if (semaphore == null) {  
		    	semaphore = new Semaphore(permits,true); 
		    	Semaphore l = SEMAPHORE_MAP.putIfAbsent(region, semaphore);  
		        if (l != null) {  
		        	semaphore = l;  
		        }  
		    }  
    		try {
    			if(permitsAcquireTimeOut <=0){
					semaphore.acquire();
					log.info("semaphore hashCode {} semaphore.acquire {} region",semaphore.hashCode(),region);
				}else{
					tryAcquireFlag = semaphore.tryAcquire(permitsAcquireTimeOut, TimeUnit.MILLISECONDS);
					log.info("semaphore hashCode {} semaphore.tryAcquire region {}  time:{} SECONDS flag :{}",semaphore.hashCode(),region,permitsAcquireTimeOut,tryAcquireFlag);
				    if(!tryAcquireFlag){
				    	throw new CacheException("semaphore.tryAcquire false");
				    }
				}
    			
    			//处理热点key，缓存雪崩
    			//String lockStr = region.intern();
    			//synchronized (lockStr) {
    				//再次检测
    				cacheObject = get(region, key);
    				if(cacheObject.getValue() != null){
    					return cacheObject;
    				}
    				log.info("get callable region="+region+",key="+key);
    				//System.out.println("get callable region="+region+",key="+key);
    				Object callObj = callable.call();
    				callObj = callObj == null ? NULL : callObj;
    				cacheObject.setValue(callObj);
    				cacheObject.setLevel(LEVEL_3);
    				set(region, key, callObj);
    				//处理缓存穿透  额外对空置设置过期时间
    				if(NULL.equals(callObj)){
    					CacheManager.expire(LEVEL_1, region, key, 60);
    					log.info("write data to cache region="+region+",key="+key+",value is null expire 60s");
    				}
				//}
			} catch (Exception e) {
				throw new CacheException(e);
			}finally{
				if(semaphore!=null && tryAcquireFlag){
					semaphore.release();
					log.info("semaphore.release {} region",region);
				}
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
    public void set(String region, Object key, Object value,boolean sycCluster) {
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
                // _sendEvictCmd(region, key);//清除原有的一级缓存的内容
                 CacheManager.set(LEVEL_1, region, key, value);
                 if(sycCluster){
                	 _sendSetCmd(region, key, value);
                 }
             }
         }
         log.info("write data to cache region="+region+",key="+key+",value="+value);
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
    }
    
    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendSetCmd(String region, Object key,Object val) {
    }

    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendEvictCmd(String region, Object key) {
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
}
