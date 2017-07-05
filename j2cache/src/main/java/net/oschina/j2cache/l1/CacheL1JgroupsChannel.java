package net.oschina.j2cache.l1;

import java.net.URL;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheManager;
import net.oschina.j2cache.CacheOprator;
import net.oschina.j2cache.Command;
import net.oschina.j2cache.l2.CacheL2BaseChannel;
import net.oschina.j2cache.util.CacheUtils;
import net.oschina.j2cache.util.ConfigUtils;
import net.oschina.j2cache.util.SerializationUtils;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 缓存多播通道
 *
 * @author zzm
 */
public class CacheL1JgroupsChannel extends CacheL1BaseChannel {

    private final static Logger log = LoggerFactory.getLogger(CacheL2BaseChannel.class);
    private final static String CONFIG_XML = "/network.xml";

    private String name;
    private JChannel channel;
    private final static CacheL1JgroupsChannel instance = new CacheL1JgroupsChannel("default");

    /**
     * 单例方法
     *
     * @return
     */
    public final static CacheL1JgroupsChannel getInstance() {
        return instance;
    }

    /**
     * 初始化缓存通道并连接
     *
     * @param name
     * @throws CacheException
     */
    private CacheL1JgroupsChannel(String name) throws CacheException {
        this.name = name;
        try {
            CacheManager.initCacheProvider(this);

            long ct = System.currentTimeMillis();
            String configPath = CacheManager.getProperties().getProperty("cache.L1.provider.jgroup.config");
            URL xml  = ConfigUtils.getURL(configPath,CONFIG_XML);
            
            channel = new JChannel(xml);
            channel.setReceiver(new JgroupsMQBinaryHandler());
            channel.connect(this.name);

            log.info("Connected to channel:" + this.name + ", time " + (System.currentTimeMillis() - ct) + " ms.");

        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

   
    public void _sendClearCmd(String region) {
        // 发送广播
        Command cmd = new Command(CacheOprator.REMOVE_ALL, region, "");
        try {
            Message msg = new Message(null, null, SerializationUtils.serialize(cmd));
            channel.send(msg);
        } catch (Exception e) {
            log.error("Unable to clear cache,region=" + region, e);
        }
    }
    
    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendSetCmd(String region, Object key,Object val) {
        try {
             //发送广播
             Command cmd = new Command(CacheOprator.SET, region, key,val);
             Message msg = new Message(null, null, SerializationUtils.serialize(cmd));
             channel.send(msg);
        } catch (Exception e) {
            log.error("Unable to delete cache,region=" + region + ",key=" + key, e);
        }
    }

    /**
     * 发送清除缓存的广播命令
     *
     * @param region
     * @param key
     */
    public void _sendEvictCmd(String region, Object key) {
        //发送广播
        Command cmd = new Command(CacheOprator.REMOVE, region, key);
        try {
            Message msg = new Message(null, null, SerializationUtils.serialize(cmd));
            channel.send(msg);
        } catch (Exception e) {
            log.error("Unable to delete cache,region=" + region + ",key=" + key, e);
        }
    }
    
    private static class JgroupsMQBinaryHandler extends ReceiverAdapter {  
    	/**
         * 消息接收
         */
        @Override
        public void receive(Message msg) {
            //无效消息
            byte[] buffers = msg.getBuffer();
            if (buffers.length < 1) {
                log.warn("Message is empty.");
                return;
            }
            CacheUtils.onMessage(buffers); 
        }
     }  

}
