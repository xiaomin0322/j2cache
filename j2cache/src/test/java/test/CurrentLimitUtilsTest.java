package test;

import java.io.InputStream;
import java.util.concurrent.Callable;

import net.oschina.j2cache.util.CurrentLimitUtils;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.util.StringUtils;

public class CurrentLimitUtilsTest {

	public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		test2();
	}
	
	
	// 缓存穿透,缓存失效模拟
	public static void test1()throws Exception {
		InputStream in = EhCacheTest.class.getClassLoader().getResourceAsStream("ehcache.xml");
		CacheManager cm = CacheManager.create(in);
		Ehcache cache = cm.getCache("session");
		
				for (int i = 0; i < 50; i++) {
					final int j = i;
					new Thread() {
						public void run() {
							String key = "zhangsan";
							String user = (String) cache.get(key).getObjectKey();
							if(StringUtils.isEmpty(user)){
								//通过信号量防止缓存失效db雪崩
								String obj = CurrentLimitUtils.get("user",
										new Callable<String>() {
											@Override
											public String call() throws Exception {
												Thread.sleep(2000);
												return "call"+j;
											}
										},2);
								System.out.println("==================================="+obj);
								 Element element = new Element(key, obj);
						          cache.put(element);
						         //如果为空设置过期时间防止缓存穿透
								if(StringUtils.isEmpty(obj)){
									 element.setTimeToLive(60);
				                     element.setTimeToIdle(60);
								}
							}
						};
					}.start();
				}
				
				//Thread.sleep(10000);
				
				
				
				//System.out.println(CurrentLimitUtils.SEMAPHORE_MAP.size());
				
			}
	
		public static void test2()throws Exception {
			for (int i = 0; i < 50; i++) {
				final int j = i;
				new Thread() {
					public void run() {
						String obj = CurrentLimitUtils.get("user",
								new Callable<String>() {
									@Override
									public String call() throws Exception {
										Thread.sleep(2000);
										return "call"+j;
									}
								},2);
						System.out.println("==================================="+obj);
						
					};
				}.start();
			}
			
			//Thread.sleep(10000);
			
			
			
			//System.out.println(CurrentLimitUtils.SEMAPHORE_MAP.size());
			
		}

}
