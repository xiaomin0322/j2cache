package net.oschina.j2cache.util;

import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.Command;
import net.oschina.j2cache.l1.CacheL1BaseChannel;
import net.oschina.j2cache.l1.CacheL1RedisChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheUtils {
	
	private final static Logger log = LoggerFactory
			.getLogger(CacheUtils.class);
	public static void onMessage(byte[] message){
		 try {
	    		Command cmd = (Command) SerializationUtils.deserialize(message);
				//不处理发送给自己的消息
		        if (cmd.getMsgId().equalsIgnoreCase(Command.ID)){
		        	  return;
		        }
		        CacheL1RedisChannel cacheL1RedisChannel = CacheL1RedisChannel.getInstance();
		        log.debug("message {}",cmd);
		        switch (cmd.getOperator()) {
		                case REMOVE:
		                	cacheL1RedisChannel.onDeleteCacheKey(cmd.getRegion(), cmd.getKey());
		                    break;
		                case REMOVE_ALL:
		                	cacheL1RedisChannel.onClearCache(cmd.getRegion());
		                    break;
		                case SET:
		                	cacheL1RedisChannel.set(cmd.getRegion(), cmd.getKey(), cmd.getVal());
		                    break;    
		                default:
		                    log.warn("Unknown message type = " + cmd.getOperator());
		            }
		        } catch (Exception e) {
		            log.error("Unable to handle received msg", e);
		       } 
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getVal(CacheObject cacheObject){
		if(cacheObject == null){
			return null;
		}
		if(cacheObject.getValue() == null || CacheL1BaseChannel.NULL.equals(cacheObject.getValue())){
			return null;
		}
		return (T) cacheObject.getValue();
	}

}
