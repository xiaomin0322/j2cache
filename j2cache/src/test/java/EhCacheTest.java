import java.io.InputStream;
import java.util.Arrays;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;


public class EhCacheTest {

	public static void main(String[] args) throws Exception{
		//加载EhCache配置文件
		InputStream in = EhCacheTest.class.getClassLoader().getResourceAsStream("ehcache.xml");
		CacheManager cm = CacheManager.create(in);
		String[] names = cm.getCacheNames();
		
		System.out.println(Arrays.toString(names));
		
		Ehcache cache = cm.getCache("session");
		Element e = new Element("aa", "aa", false, 2, 2);
		cache.put(e);
		System.out.println(cache.get("aa"));
		Thread.sleep(2050);
		System.out.println(cache.get("aa"));//如果这个时候，期待cache是否过期。但是实际的情况是。ehcache依然能获取到相关数据.
		
		
	}

}
