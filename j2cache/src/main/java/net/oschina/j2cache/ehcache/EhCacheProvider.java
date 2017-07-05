//$Id: EhCacheProvider.java 9964 2006-05-30 15:40:54Z epbernard $
/**
 *  Copyright 2003-2006 Greg Luck, Jboss Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.oschina.j2cache.ehcache;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheExpiredListener;
import net.oschina.j2cache.CacheProvider;
import net.oschina.j2cache.util.ConfigUtils;
import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache Provider plugin
 * <p/>
 * Taken from EhCache 1.3 distribution
 *
 * @author liudong
 */
public class EhCacheProvider implements CacheProvider {

    private final static Logger log = LoggerFactory.getLogger(EhCacheProvider.class);
    private final static String CONFIG_XML = "/ehcache.xml";

    private CacheManager manager;
    private ConcurrentHashMap<String, EhCache> _CacheManager;

    @Override
    public String name() {
        return "ehcache";
    }

    /***
     * Builds a Cache.
     * <p/>
     * Even though this method provides properties, they are not used.
     * Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration
     * where the name attribute matches the name parameter in this builder.
     * @param name  the name of the cache. Must match a cache configured in ehcache.xml
     * @param autoCreate autoCreate settings
     * @param listener   listener for expired elements
     * @return a newly built cache will be built and initialised
     * @throws CacheException
     */
    public EhCache buildCache(String name, boolean autoCreate, CacheExpiredListener listener) throws CacheException {
        EhCache ehcache = _CacheManager.get(name);
        if (ehcache == null && autoCreate) {
            try {
                synchronized (_CacheManager) {
                    ehcache = _CacheManager.get(name);
                    if (ehcache == null) {
                        net.sf.ehcache.Cache cache = manager.getCache(name);
                        if (cache == null) {
                            log.warn("Could not find configuration [" + name + "]; using defaults.");
                            manager.addCache(name);
                            cache = manager.getCache(name);
                            log.debug("started EHCache region: " + name);
                        }
                        ehcache = new EhCache(cache, listener);
                        _CacheManager.put(name, ehcache);
                    }
                }
            } catch (net.sf.ehcache.CacheException e) {
                throw new CacheException(e);
            }
        }
        return ehcache;
    }

    /**
     * Callback to perform any necessary initialization of the underlying cache implementation
     * during SessionFactory construction.
     *
     * @param props current configuration settings.
     */
    public void start(Properties props) throws CacheException {
        if (manager != null) {
            log.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() " +
                    " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider." +
                    " If this behaviour is required, consider using net.sf.ehcache.hibernate.SingletonEhCacheProvider.");
            return;
        }
        String configPath  = props.getProperty("cache.L1.provider.ehcache.config");
        URL xml  = ConfigUtils.getURL(configPath,CONFIG_XML);
        manager = CacheManager.create(xml);
        _CacheManager = new ConcurrentHashMap<String, EhCache>();
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache implementation
     * during SessionFactory.close().
     */
    public void stop() {
        if (manager != null) {
            manager.shutdown();
            manager = null;
        }
    }
    
    public static void main(String[] args)throws Exception {
    	URL xml =new URL("file","","D:\\workspace\\j2cache\\j2cache\\src\\main\\resources\\ehcache.xml");
    	xml = new URL("http://michael-paul.iteye.com/blog/1387806");
    	System.out.println(xml.openStream());
	}

}
