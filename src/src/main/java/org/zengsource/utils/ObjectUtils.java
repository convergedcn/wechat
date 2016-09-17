/**
 * 
 */
package org.zengsource.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.apache.commons.beanutils.BeanUtils;

/**
 * @author zengsn
 * @since 8.0
 */
public class ObjectUtils {
	
	public static boolean different(Object obj1, Object obj2, String excludes) {
		Field[] fields = obj1.getClass().getDeclaredFields();
		// 检查成员变量
		for (Field field : fields) {
			if (field.getModifiers() < Modifier.STATIC) {
				if (!excludes.contains(field.getName())) {
					try {
						Object value1 = BeanUtils.getProperty(obj1, field.getName());
						Object value2 = BeanUtils.getProperty(obj2, field.getName());
						if (field.getType().equals(String.class)) {
							if (value1 == null) {
								value1 = "";
							}
							if (value2 == null) {
								value2 = "";
							}
						}
						if (value1 == null || !value1.equals(value2)) {
							return true; // 不一样
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

}
