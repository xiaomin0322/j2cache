package test;

import java.util.concurrent.Callable;

import net.oschina.j2cache.util.CurrentLimitUtils;

public class CurrentLimitUtilsTest {

	public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		test2();
	}
	
	// 缓存穿透测试
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
