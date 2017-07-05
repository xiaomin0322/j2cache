package net.oschina.j2cache.factory;

import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.l1.CacheL1JgroupsChannel;
import net.oschina.j2cache.l1.CacheL1RedisChannel;
import net.oschina.j2cache.l2.CacheL2BaseChannel;

public class CacheFactory {

	/**
	 * 获取一级缓存基于rdis消息通讯
	 * @return
	 */
	public static ICacheChannel getCacheL1RedisChannel(){
		return CacheL1RedisChannel.getInstance();
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
	public static ICacheChannel ICacheChannel(){
		return CacheL2BaseChannel.getInstance().setCacheL1Channel(CacheL1RedisChannel.getInstance());
	}
}
