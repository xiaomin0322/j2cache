package net.oschina.j2cache.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class SpringProperty extends PropertyPlaceholderConfigurer {
	private static Map<String, String> propertiesMap = new HashMap<String, String>();
	
	private static Properties properties;

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			Properties props) throws BeansException {
		properties = props;
		super.processProperties(beanFactoryToProcess, props);
		for (Entry<Object, Object> entry : props.entrySet()) {
			propertiesMap.put(entry.getKey().toString(), entry.getValue().toString());
		}
	}
	
	public static Properties getProps(){
		return properties;
	}

	public static Map<String, String> getProperties() {
		return propertiesMap;
	}

	public static String getProperty(String key) {
		return propertiesMap.get(key);
	}
}
