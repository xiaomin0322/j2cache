package net.oschina.j2cache.l1;

import java.util.concurrent.Callable;

import net.oschina.j2cache.ICacheChannel;

public class CacheL1Test {
	
	public static void main(String[] args) {
		test1();
	}
	
	//热点key失效，缓存雪崩测试
	public static void test1(){
		ICacheChannel cache = CacheL1RedisChannel.getInstance();
		for(int i=0;i<50;i++){
			new Thread(){
				public void run() {
					Object obj = cache.get("zzm", "test", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return "123";
						}
					});
					System.out.println(obj);
				};
			}.start();
		}
	}

}
