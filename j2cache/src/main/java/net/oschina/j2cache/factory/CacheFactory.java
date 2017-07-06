package net.oschina.j2cache.factory;

import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.l1.CacheL1BaseChannel;
import net.oschina.j2cache.l1.CacheL1JgroupsChannel;
import net.oschina.j2cache.l1.CacheL1RedisChannel;
import net.oschina.j2cache.l1.CacheL1RedisClusterChannel;
import net.oschina.j2cache.l2.CacheL2BaseChannel;
import net.oschina.j2cache.l2.CacheL2Channel;

public class CacheFactory {

	/**
	 * 获取一级缓存基于rdis消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL1RedisChannel(){
		String l2_provider_class = CacheManager.getProperties().getProperty("cache.L2.provider_class");
        		if("redis".equalsIgnoreCase(l2_provider_class)){
        			return CacheL1RedisChannel.getInstance();
        		}else{
        			return CacheL1RedisClusterChannel.getInstance();
        		}
	}
	
	/**
	 * 获取一级缓存基于jgroup消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL1JgroupsChannel(){
		return CacheL1JgroupsChannel.getInstance();
	}
	

	/**
	 * 获取一级缓存基于jgroup消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL1JgroupsAndL2Redis(){
		return CacheL2BaseChannel.getInstance().setCacheL1Channel( CacheL1JgroupsChannel.getInstance());
	}
	
	/**
	 * 获取一级缓存基于redis消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL1ReidsAndL2Redis(){
		CacheL1BaseChannel cacheL1BaseChannel = ((CacheL1BaseChannel)getCacheL1RedisChannel());
		return CacheL2BaseChannel.getInstance().setCacheL1Channel(cacheL1BaseChannel);
	}
	
	/**
	 * 获取一级缓存基于redis消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL2Channel(){
		return CacheL2Channel.getInstance();
	}
}
