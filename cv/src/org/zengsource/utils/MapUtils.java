/**
 * 
 */
package org.zengsource.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

/**
 * @author zengsn
 * @since 8.0
 */
public abstract class MapUtils {

	public static void toObject(Map<String, Object> map, Object object) {
		Field[] fields = object.getClass().getDeclaredFields();
		for (Field field : fields) {
			String name = field.getName();
			Object value = getValue(map, name);
			try {
				BeanUtils.setProperty(object, name, value);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	/** 忽略Key的大小写 */
	public static Object getValue(Map<String, Object> map, String keyCaseInsensitive) {
		for (String key : map.keySet()) {
			if (key.toLowerCase().equals( //
					keyCaseInsensitive.toLowerCase())) {
				return map.get(key);
			}
		}
		return null;
	}

	public static void toMap(Object object, Map<String, Object> map) {
		Field[] fields = object.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.getModifiers() < Modifier.STATIC) {
				String name = field.getName();
				try {
					Object value = BeanUtils.getProperty(object, name);
					map.put(name, value);
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

	public static Map<String, Object> toMap(Object object) {
		Map<String, Object> map = new HashMap<String, Object>();
		toMap(object, map);
		return map;
	}

	public static <T extends Object> List<Map<String, Object>> toMapList(List<T> objects) {
		if (objects != null) {
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			for (T object : objects) {
				list.add(MapUtils.toMap(object));
			}
			return list;
		}
		return null;
	}

	/** 支持大小写无关的合并Map */
	public static void putAll(Map<String, Object> map1, Map<String, Object> map2, boolean caseInsensitive) {
		if (caseInsensitive) {
			for (String key2 : map2.keySet()) {
				put(map1, key2, map2.get(key2), caseInsensitive);
			}
		} else {
			map1.putAll(map2);
		}

	}

	public static void put(Map<String, Object> map, String key, Object value, boolean caseInsensitive) {
		if (caseInsensitive) {
			String matchedKey = null;
			for (String k : map.keySet()) {
				if (k.toLowerCase().equals(key.toLowerCase())) {
					matchedKey = k; // 找到相同的Key
					map.put(k, value); // 忽略大小写
					break;
				}
			}
			// 原来没有这个Key
			if (matchedKey == null) {
				map.put(key, value);
			}
		} else {
			map.put(key, value);
		}

	}

}
