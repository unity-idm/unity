/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;

/**
 * Defines common part of template used for sending credential reset confirmation code.
 * @author P. Piernik
 * 
 */
@Component
public class CredentialResetTemplateDefBase
{
	public static final String VAR_USER = "user";
	public static final String VAR_CODE = "code";
	
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<String, MessageTemplateVariable>();
		vars.put(VAR_USER, new MessageTemplateVariable(VAR_USER, "MessageTemplateConsumer.PasswordReset.var.user", false));
		vars.put(VAR_CODE, new MessageTemplateVariable(VAR_CODE, "MessageTemplateConsumer.PasswordReset.var.code", false));
		return vars;
	}
}
