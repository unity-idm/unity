/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.message;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.base.i18n.I18nString;

public interface MessageSource extends org.springframework.context.MessageSource
{
	public static final String PROFILE_FAIL_ON_MISSING = "failOnMissingMessage";
	
	String getMessage(String code, Object... args);
	String getMessageUnsafe(String code, Object... args);
	String getMessageNullArg(String code, Object... args);
	String getYesNo(boolean value);
	Locale getLocale();
	String getDefaultLocaleCode();
	String getLocaleCode();
	Map<String, Locale> getEnabledLocales();
	Map<String, Locale> getSupportedLocales();	
	
	Map<MessageArea, Set<Object>> getKeysByCategory();
	I18nString getI18nMessage(String code, Object... args);
	
}
