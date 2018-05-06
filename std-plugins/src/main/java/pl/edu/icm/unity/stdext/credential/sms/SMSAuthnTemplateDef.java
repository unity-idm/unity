/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.sms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.FacilityName;

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
	public Set<String> getCompatibleFacilities()
	{
		return Collections.unmodifiableSet(Sets.newHashSet(FacilityName.SMS.toString()));

	}
}
