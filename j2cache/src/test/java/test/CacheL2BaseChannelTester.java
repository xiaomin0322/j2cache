package test;
/**
 *
 */


import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.ICacheChannel;
import net.oschina.j2cache.factory.CacheFactory;

/**
 * 缓存测试入口
 *
 * @author Winter Lau
 */
public class CacheL2BaseChannelTester {

    public static void main(String[] args) {
              test();
    }
    
    public static void test(){
    	System.setProperty("java.net.preferIPv4Stack", "true"); //Disable IPv6 in JVM

        //ICacheChannel cache = CacheFactory.getCacheL1JgroupsAndL2Redis();
        ICacheChannel cache = CacheFactory.getCacheL1ReidsAndL2Redis();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        do {
            try {
                System.out.print("> ");
                System.out.flush();

                String line = in.readLine();
                if (null == line || line.trim().equalsIgnoreCase("quit") || line.trim().equalsIgnoreCase("exit"))
                    break;

                String[] cmds = line.trim().split(" ");
                if ("get".equalsIgnoreCase(cmds[0])) {
                    CacheObject obj = cache.get(cmds[1], cmds[2]);
                    System.out.printf("[%s,%s,L%d]=>%s\n", obj.getRegion(), obj.getKey(), obj.getLevel(), obj.getValue());
                } else if ("set".equalsIgnoreCase(cmds[0])) {
                	if(cmds.length == 5){
                		cache.set(cmds[1], cmds[2], cmds[3],Boolean.valueOf(cmds[4]));
                		 System.out.printf("[%s,%s]<=%s\n", cmds[1], cmds[2], cmds[3],cmds[4]);
                	}else{
                		 cache.set(cmds[1], cmds[2], cmds[3]);
                		 System.out.printf("[%s,%s]<=%s\n", cmds[1], cmds[2], cmds[3]);
                	}
                } else if ("evict".equalsIgnoreCase(cmds[0])) {
                    cache.evict(cmds[1], cmds[2]);
                    System.out.printf("[%s,%s]=>null\n", cmds[1], cmds[2]);
                } else if ("clear".equalsIgnoreCase(cmds[0])) {
                    cache.clear(cmds[1]);
                    System.out.printf("Cache [%s] clear.\n", cmds[1]);
                } else if ("help".equalsIgnoreCase(cmds[0])) {
                    printHelp();
                } else {
                    System.out.println("Unknown command.");
                    printHelp();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Wrong arguments.");
                printHelp();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (true);

        cache.close();

        System.exit(0);
    }

    private static void printHelp() {
        System.out.println("Usage: [cmd] region key [value]");
        System.out.println("cmd: get/set/evict/quit/exit/help");
    }

}
