package net.oschina.j2cache.util;

import java.net.MalformedURLException;
import java.net.URL;

import net.oschina.j2cache.CacheException;

public class ConfigUtils {
	

	public static URL getURL(String remoteConfigPath,String localConfigPath){
		URL xml  = null;
		 if(remoteConfigPath != null || "".equals(remoteConfigPath)){
	        	try {
	        		if(remoteConfigPath.startsWith("http")){
	        			xml = new URL(remoteConfigPath);
	        		}else{
	        			xml =new URL("file","",remoteConfigPath);
	        		}
				} catch (MalformedURLException e) {
					throw new CacheException("find ehcache.xml exception !!! "+remoteConfigPath);
				}
	        }else{
	        	  xml =ConfigUtils.class.getClassLoader().getParent().getResource(localConfigPath);
	             if (xml == null)
	                 xml = ConfigUtils.class.getResource(localConfigPath);
	             if (xml == null)
	                 throw new CacheException("cannot find ehcache.xml !!!");
	        }
           return xml;
	}
}
