/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;

public class I18nTextFieldTest
{
	@Test
	public void preservesValue()
	{
		UnityMessageSource msg = mock(UnityMessageSource.class);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		Map<String, Locale> locales = new HashMap<>();
		locales.put("english", new Locale("en"));
		locales.put("polski", new Locale("pl"));
		when(msg.getEnabledLocales()).thenReturn(locales);
		
		I18nTextField tf= new I18nTextField(msg);
		I18nString original = new I18nString("a");
		original.addValue("en", "Value 1");
		original.addValue("pl", "Wartość 1");
		tf.setValue(original);
		I18nString ret = tf.getValue();
		Assert.assertEquals(original, ret);
	}
	
}
