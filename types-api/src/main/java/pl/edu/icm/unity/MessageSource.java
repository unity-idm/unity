/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import java.util.Locale;
import java.util.Map;

/**
 * Interface implemented by UnityMessageSource. This interface exists only to be able to use i19n in this 
 * Maven module, as UnityMessageSource is not and won't be available here.
 * @author K. Benedyczak
 */
public interface MessageSource extends org.springframework.context.MessageSource
{
	String getMessage(String code, Object... args);
	String getMessageUnsafe(String code, Object... args);
	String getMessageNullArg(String code, Object... args);
	String getYesNo(boolean value);
	Locale getLocale();
	String getDefaultLocaleCode();
	String getLocaleCode();
	Map<String, Locale> getEnabledLocales();
	Map<String, Locale> getSupportedLocales();
}
