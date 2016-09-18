/**
 * 
 */
package org.zengsource.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zengsn
 * @since 8.0
 */
public class RegexUtils {

	public static boolean matches(String regex, String input) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(input);
		return matcher.matches();
	}

}
