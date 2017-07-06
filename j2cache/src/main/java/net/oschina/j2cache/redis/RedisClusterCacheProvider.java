package net.oschina.j2cache.redis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.oschina.j2cache.Cache;
import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheExpiredListener;
import net.oschina.j2cache.CacheProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis集群 缓存实现
 *
 * @author zzm
 */
@SuppressWarnings("all")
public class RedisClusterCacheProvider implements CacheProvider {
	 private final static Logger log = LoggerFactory.getLogger(RedisClusterCacheProvider.class);
    private static String host;
   
	private static int port;
    private static int timeout;
    private static String password;
    private static int database;

    private static JedisCluster cluster;

    @Override
    public String name() {
        return "cache.redis";
    }


    public static JedisCluster getResource() {
        return cluster;
    }
    
    @Override
    public Cache buildCache(String regionName, boolean autoCreate, CacheExpiredListener listener) throws CacheException {
        return new RedisCache(regionName);
    }

    @Override
    public void start(Properties props) throws CacheException {
        JedisPoolConfig config = new JedisPoolConfig();

        host = getProperty(props, "host", "127.0.0.1");
        password = props.getProperty("password", null);

       // port = getProperty(props, "port", 6379);
        timeout = getProperty(props, "timeout", 2000);
        database = getProperty(props, "database", 0);

        config.setBlockWhenExhausted(getProperty(props, "whenExhaustedAction", true));
        config.setMaxIdle(getProperty(props, "maxIdle", 10));
        config.setMinIdle(getProperty(props, "minIdle", 5));
        config.setMaxTotal(getProperty(props, "maxActive", 50));
        config.setMaxWaitMillis(getProperty(props, "maxWait", 100));
        config.setTestWhileIdle(getProperty(props, "testWhileIdle", false));
        config.setTestOnBorrow(getProperty(props, "testOnBorrow", true));
        config.setTestOnReturn(getProperty(props, "testOnReturn", false));
        config.setNumTestsPerEvictionRun(getProperty(props, "numTestsPerEvictionRun", 10));
        config.setMinEvictableIdleTimeMillis(getProperty(props, "minEvictableIdleTimeMillis", 1000));
        config.setSoftMinEvictableIdleTimeMillis(getProperty(props, "softMinEvictableIdleTimeMillis", 10));
        config.setTimeBetweenEvictionRunsMillis(getProperty(props, "timeBetweenEvictionRunsMillis", 10));
        config.setLifo(getProperty(props, "lifo", false));

        int maxRedirections = getProperty(props, "maxRedirections", 6);
        
        Set<HostAndPort> nodes = parseHostAndPort();
         cluster = new JedisCluster(nodes,timeout,maxRedirections,config);  

    }
    
    public static TreeSet<String> keys(String pattern){
		log.debug("Start getting keys...");
		TreeSet<String> keys = new TreeSet<>();
		Map<String, JedisPool> clusterNodes =cluster.getClusterNodes();
		for(String k : clusterNodes.keySet()){
			log.debug("Getting keys from: {}", k);
			JedisPool jp = clusterNodes.get(k);
			Jedis connection = jp.getResource();
			try {
				keys.addAll(connection.keys(pattern));
			} catch(Exception e){
				log.error("Getting keys error: {}", e);
			} finally{
				log.debug("Connection closed.");
				connection.close();//用完一定要close这个链接！！！
			}
		}
		log.debug("Keys gotten!");
		return keys;
	}
    
    private Set<HostAndPort> parseHostAndPort(){
		try {
			if(StringUtils.isEmpty(host)){
				log.error("addressConfig isEmpty >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				return null;
			}
			
			Set<HostAndPort> haps = new HashSet<HostAndPort>();
			for (String server:host.split(",")) {
				String[] ipAndPort = server.split(":");
				HostAndPort hap = new HostAndPort(ipAndPort[0],
						Integer.parseInt(ipAndPort[1]));
				haps.add(hap);
			}

			return haps;
		} catch (Exception ex) {
			log.error("解析 jedis 配置文件失败", ex);
		}
		return null;
	}

    @Override
    public void stop() {
    	try {
			cluster.close();
		} catch (IOException e) {
			log.error("stop cluster error", e);
		}
    }

    private static String getProperty(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue).trim();
    }

    private static int getProperty(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static boolean getProperty(Properties props, String key, boolean defaultValue) {
        return "true".equalsIgnoreCase(props.getProperty(key, String.valueOf(defaultValue)).trim());
    }
}
