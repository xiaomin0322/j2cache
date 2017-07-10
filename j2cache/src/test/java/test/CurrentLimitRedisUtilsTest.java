package test;

import java.util.concurrent.Callable;

import net.oschina.j2cache.factory.CacheFactory;
import net.oschina.j2cache.util.CurrentLimitRedisUtils;

public class CurrentLimitRedisUtilsTest {

	public static void main(String[] args)throws Exception {
	   CacheFactory.getCacheL1RedisChannel();
		// TODO Auto-generated method stub
		test1();
	}
	
	// 缓存穿透,缓存失效模拟
	public static void test1()throws Exception {
				for (int i = 0; i < 50; i++) {
					final int j = i;
					new Thread() {
						public void run() {
							String key = "zhangsan"+j;
							CurrentLimitRedisUtils.get("locakName", key, new Callable<String>() {
								public String call() throws Exception {
									String rs = "查询结果为："+j;
									System.out.println("查询数据库结果为："+rs);
									Thread.sleep(2000);
									return rs;
								};
							}, 2);
							
						};
					}.start();
				}
			}
	

}
