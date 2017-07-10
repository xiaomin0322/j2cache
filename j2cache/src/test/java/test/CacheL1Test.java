package test;




import java.util.concurrent.Callable;

import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.factory.CacheFactory;
import net.oschina.j2cache.l1.CacheL1RedisChannel;

public class CacheL1Test {

	public static void main(String[] args) throws Exception{
		test1();
	}

	// 缓存穿透测试
	public static void test2()throws Exception {
		ICacheChannel cache = CacheFactory.getCacheL1RedisChannel();
		for (int i = 0; i < 50; i++) {
			new Thread() {
				public void run() {
					CacheObject obj = cache.get("user", "test",
							new Callable<String>() {
								@Override
								public String call() throws Exception {
									return null;
								}
							});
					System.out.println(obj.getValue());
				};
			}.start();
		}
		
		Thread.sleep(30000);
		CacheObject obj = cache.get("user", "test",
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						return "123";
					}
				});
		System.out.println("过了30秒查询结果："+obj.getValue());
		
		Thread.sleep(31000);
		 obj = cache.get("zzm", "test",
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						return "123";
					}
				});
		System.out.println("过了60秒查询结果："+obj.getValue());
		
	}

	// 热点key失效，缓存雪崩测试
	public static void test1() {
		ICacheChannel cache = CacheL1RedisChannel.getInstance();
		for (int i = 0; i < 50; i++) {
			new Thread() {
				public void run() {
					Object obj = cache.get("user", "test",
							new Callable<String>() {
								@Override
								public String call() throws Exception {
									return "123";
								}
							});
					System.out.println("cache.get ==="+obj);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				};
			}.start();
		}
	}

}
