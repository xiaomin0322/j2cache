package spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import test.CacheL1Tester;


public class BaseSpringApp {
    public  static ApplicationContext                 context=null; 
    static{
        context= new ClassPathXmlApplicationContext(new String[]{"classpath:applicationContext.xml" });
    }
    public static void main(String[] args) {
    	//CacheL2BaseChannelTester.test();
    	CacheL1Tester.test();
	}
}
