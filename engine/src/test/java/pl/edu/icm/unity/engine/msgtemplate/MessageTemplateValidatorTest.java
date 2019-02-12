/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;

public class MessageTemplateValidatorTest
{
	@Test
	public void shouldFindWithoutDefaultVariant()
	{
		I18nString subject = new I18nString("en", "{{var}}");
		I18nString body = new I18nString("en", "{{var2}}");
		I18nMessage message = new I18nMessage(subject, body);
		
		Set<String> variables = MessageTemplateValidator.extractVariables(message);
		
		assertThat(variables.toString(), variables, hasItems("var", "var2"));
	}
	
	@Test
	public void shouldFindOldAndNewVariables()
	{
		I18nString subject = new I18nString("Subject{{sNewVar}}${sOldVar}end");
		I18nString body = new I18nString("Body{{bNewVar}}${bOldVar}end");
		I18nMessage message = new I18nMessage(subject, body);
		
		Set<String> variables = MessageTemplateValidator.extractVariables(message);
		
		assertThat(variables.toString(), variables, hasItems("sNewVar", "sOldVar", "bNewVar", "bOldVar"));
	}
	
	@Test
	public void shouldFindWithSpecialChars()
	{
		I18nString subject = new I18nString("Subject${weird.name-foo!$}end");
		I18nString body = new I18nString("Body{{new%name-foo!$}}D");
		I18nMessage message = new I18nMessage(subject, body);
		
		Set<String> variables = MessageTemplateValidator.extractVariables(message);
		
		assertThat(variables.toString(), variables, hasItems("weird.name-foo!$", "new%name-foo!$"));
	}
}
