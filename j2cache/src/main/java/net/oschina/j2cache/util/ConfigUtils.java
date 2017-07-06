package net.oschina.j2cache.util;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.ehcache.EhCacheProvider;

public class ConfigUtils {
	 private final static Logger log = LoggerFactory.getLogger(ConfigUtils.class);
	public static URL getURL(String remoteConfigPath, String localConfigPath) {
		URL xml = null;
		if (remoteConfigPath != null && !"".equals(remoteConfigPath)) {
			try {
				if (remoteConfigPath.startsWith("http")) {
					xml = new URL(remoteConfigPath);
				} else {
					xml = new URL("file", "", remoteConfigPath);
				}
				log.info("load remoteConfigPath >>>>>> {}",remoteConfigPath);
			} catch (MalformedURLException e) {
				throw new CacheException("find ehcache.xml exception !!! "
						+ remoteConfigPath);
			}
		} else {
			log.info("load localConfigPath >>>>>> {}",localConfigPath);
			xml = Thread.currentThread().getContextClassLoader().getResource(localConfigPath);
			if (xml == null)
				throw new CacheException("cannot find ehcache.xml !!! "+localConfigPath);
		}
		return xml;
	}

	public static InputStream getInputStream(String remoteConfigPath,
			String localConfigPath) {
		InputStream xml = null;
		try {
			xml  = getURL(remoteConfigPath, localConfigPath).openStream();
		} catch (Exception e) {
			  throw new CacheException("getInputStream Exception", e);
		}

		return xml;
	}

}
