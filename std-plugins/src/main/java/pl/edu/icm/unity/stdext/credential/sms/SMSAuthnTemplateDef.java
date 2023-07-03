/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.sms;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msg_template.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * Defines template used for sending authn sms code by mobile.
 * @author P. Piernik
 * 
 */
@Component
public class SMSAuthnTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "SMSAuthn";
	
	public static final String VAR_USER = "user";
	public static final String VAR_CODE = "code";
	
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<String, MessageTemplateVariable>();
		vars.put(VAR_USER, new MessageTemplateVariable(VAR_USER, "MessageTemplateConsumer.SMSAuthn.var.user", false));
		vars.put(VAR_CODE, new MessageTemplateVariable(VAR_CODE, "MessageTemplateConsumer.SMSAuthn.var.code", false));
		return vars;
	}

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.SMSAuthn.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		 return EnumSet.of(CommunicationTechnology.SMS);
	}
}
