package test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import net.oschina.j2cache.factory.CacheFactory;
import net.oschina.j2cache.util.CurrentLimitRedisUtils;

public class CurrentLimitRedisUtilsTest {



	public static void main(String[] args)throws Exception {
	   CacheFactory.getCacheL1RedisChannel();
		// TODO Auto-generated method stub
	   test3();
	}
	
	// 缓存穿透,缓存失效模拟
	public static void test1()throws Exception {
				for (int i = 0; i < 50; i++) {
					final int j = i;
					new Thread() {
						public void run() {
							String key = j+"zhangsan"+j;
							String rsCall = CurrentLimitRedisUtils.get("locakName", key, new Callable<String>() {
								public String call() throws Exception {
									String rs = "查询结果为："+j;
									System.out.println("查询数据库结果为："+rs);
									Thread.sleep(2000);
									return rs;
								};
							}, 2);
							System.out.println(" CurrentLimitRedisUtils.get key =="+key +" ===="+rsCall);
						};
					}.start();
				}
			}
	
	
	// 缓存穿透,缓存失效模拟
		public static void test2()throws Exception {
					for (int i = 0; i < 50; i++) {
						final int j = i;
						new Thread() {
							public void run() {
								String key = j+"zhangsan"+j+1;
								Test rsCall = CurrentLimitRedisUtils.get("locakName", key, new Callable<Test>() {
									public Test call() throws Exception {
										String rs = "查询结果为："+j;
										Test test = new Test();
										test.name = rs;
										System.out.println("查询数据库结果为："+rs);
										Thread.sleep(2000);
										return test;
									};
								}, 2);
								System.out.println(" CurrentLimitRedisUtils.get key =="+key +" ===="+rsCall);
							};
						}.start();
					}
				}
		
		/**
		 * 测试为null的情况
		 * @throws Exception
		 */
		public static void test3()throws Exception {
						String key = "zhangsan100011";
						Test rsCall = CurrentLimitRedisUtils.get("locakName", key, new Callable<Test>() {
							public Test call() throws Exception {
								System.out.println("我被调用了我是null>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
								return null;
							};
						}, 2);
						System.out.println(" CurrentLimitRedisUtils.get key =="+key +" ===="+rsCall);
			}
}

class Test implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name = "123";
	
	@Override
	public String toString() {
		return name;
	}
}
