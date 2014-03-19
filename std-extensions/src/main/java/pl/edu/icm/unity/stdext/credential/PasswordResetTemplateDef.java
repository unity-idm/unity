/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;

/**
 * Defines template used for sending password reset confirmation code.
 * @author P. Piernik
 * 
 */
@Component
public class PasswordResetTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "PasswordResetCode";
	public static final String VAR_USER = "user";
	public static final String VAR_CODE = "code";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.PasswordReset.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = new HashMap<String, String>();
		vars.put(VAR_USER, "MessageTemplateConsumer.PasswordReset.var.user");
		vars.put(VAR_CODE, "MessageTemplateConsumer.PasswordReset.var.code");
		return vars;
	}
}
