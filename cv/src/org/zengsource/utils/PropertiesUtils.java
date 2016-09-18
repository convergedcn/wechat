/**
 * 
 */
package org.zengsource.utils;

import java.util.Properties;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * 读取 .properties 文件
 * 
 * @author zengsn
 * @since 8.0
 */
public abstract class PropertiesUtils {

	// ~ 静态成员 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	private static final String DOLLAR = "$";
	private static final String BRACE_LEFT = "{";
	private static final String BRACE_RIGHT = "}";

	// ~ 静态方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	public static Object get(Properties properties, String key, Class<?> type) {
		if (properties != null) {
			key = refineKey(key);
			String value = properties.getProperty(key);
			if (Integer.class.equals(type)) {
				return NumberUtils.createInteger(value);
			} else if (Boolean.class.equals(type)) {
				return Boolean.getBoolean(value);
			} else { // 默认为字符串
				return value;
			}
		}
		return null;
	}

	private static String refineKey(String key) {
		if (isRefKey(key)) { // 去掉 ${ 和 }
			key = key.substring(2, key.length() - 1);
		}
		return key;
	}

	/** 引用 Key 的格式为：${abc.def} */
	public static boolean isRefKey(String key) {
		return key != null //
				&& key.startsWith(DOLLAR + BRACE_LEFT) //
				&& key.endsWith(BRACE_RIGHT);
	}

	// ~ 成员变量 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 构造方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 成员方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ g^setX ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ main() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

}
