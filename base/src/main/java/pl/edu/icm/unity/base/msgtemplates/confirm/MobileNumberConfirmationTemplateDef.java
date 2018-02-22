/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates.confirm;

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
 * Message template definition for mobile number confirmation subsystem
 *   
 * @author P. Piernik
 */
@Component
public class MobileNumberConfirmationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "MobileNumberConfirmation";
	public static final String CONFIRMATION_CODE = "confirmationCode";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.MobileNumberConfirmation.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<>();
		vars.put(CONFIRMATION_CODE, new MessageTemplateVariable(CONFIRMATION_CODE,
				"MessageTemplateConsumer.MobileNumberConfirmation.var.confirmationCode",
				true));
		return vars;
	}

	@Override
	public Set<String> getCompatibleFacilities()
	{
		return Collections.unmodifiableSet(Sets.newHashSet(FacilityName.SMS.toString()));
	}
}
