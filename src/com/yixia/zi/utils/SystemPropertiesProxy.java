package com.yixia.zi.utils;

import java.lang.reflect.Method;

public class SystemPropertiesProxy {

	/**
	 * This class cannot be instantiated
	 */
	private SystemPropertiesProxy() {
	}

	/**
	 * Get the value for the given key.
	 * 
	 * @return an empty string if the key isn't found
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static String get(String key) throws IllegalArgumentException {

		String ret = "";
		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class };
			Method get = SystemProperties.getMethod("get", paramTypes);

			//Parameters
			Object[] params = { key };
			ret = (String) get.invoke(SystemProperties, params);

		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			ret = "";
			//TODO
		}

		return ret;

	}

	/**
	 * Get the value for the given key.
	 * 
	 * @return if the key isn't found, return def if it isn't null, or an empty
	 * string otherwise
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static String get(String key, String def) throws IllegalArgumentException {

		String ret = def;

		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class, String.class };
			Method get = SystemProperties.getMethod("get", paramTypes);

			//Parameters
			Object[] params = { key, def };
			ret = (String) get.invoke(SystemProperties, params);

		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			ret = def;
			//TODO
		}

		return ret;

	}

	/**
	 * Get the value for the given key, and return as an integer.
	 * 
	 * @param key the key to lookup
	 * @param def a default value to return
	 * @return the key parsed as an integer, or def if the key isn't found or
	 * cannot be parsed
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static Integer getInt(String key, int def) throws IllegalArgumentException {

		Integer ret = def;

		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class, int.class };
			Method getInt = SystemProperties.getMethod("getInt", paramTypes);

			//Parameters
			Object[] params = { key, def };
			ret = (Integer) getInt.invoke(SystemProperties, params);

		} catch (IllegalArgumentException IAE) {
			throw IAE;
		} catch (Exception e) {
			ret = def;
			//TODO
		}

		return ret;

	}

	/**
	 * Get the value for the given key, and return as a long.
	 * 
	 * @param key the key to lookup
	 * @param def a default value to return
	 * @return the key parsed as a long, or def if the key isn't found or cannot
	 * be parsed
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static Long getLong(String key, long def) throws IllegalArgumentException {

		Long ret = def;

		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class, long.class };
			Method getLong = SystemProperties.getMethod("getLong", paramTypes);

			//Parameters
			Object[] params = { key, def };
			ret = (Long) getLong.invoke(SystemProperties, params);

		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			ret = def;
			//TODO
		}

		return ret;

	}

	/**
	 * Get the value for the given key, returned as a boolean. Values 'n', 'no',
	 * '0', 'false' or 'off' are considered false. Values 'y', 'yes', '1', 'true'
	 * or 'on' are considered true. (case insensitive). If the key does not exist,
	 * or has any other value, then the default result is returned.
	 * 
	 * @param key the key to lookup
	 * @param def a default value to return
	 * @return the key parsed as a boolean, or def if the key isn't found or is
	 * not able to be parsed as a boolean.
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static Boolean getBoolean(String key, boolean def) throws IllegalArgumentException {
		Boolean ret = def;
		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class, boolean.class };
			Method getBoolean = SystemProperties.getMethod("getBoolean", paramTypes);

			//Parameters         
			Object[] params = { key, def };
			ret = (Boolean) getBoolean.invoke(SystemProperties, params);

		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			ret = def;
			//TODO
		}

		return ret;

	}

	/**
	 * Set the value for the given key.
	 * 
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 * @throws IllegalArgumentException if the value exceeds 92 characters
	 */
	public static void set(String key, String val) throws IllegalArgumentException {

		try {
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = { String.class, String.class };
			Method set = SystemProperties.getMethod("set", paramTypes);

			//Parameters         
			Object[] params = { key, val };
			set.invoke(SystemProperties, params);
		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			//TODO
		}

	}
}