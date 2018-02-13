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
 * Message template definition for confirmation subsystem
 *   
 * @author P. Piernik
 */
@Component
public class EmailConfirmationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "EmailConfirmation";
	public static final String CONFIRMATION_LINK = "confirmationLink";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.EmailConfirmation.desc";
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
		vars.put(CONFIRMATION_LINK, new MessageTemplateVariable(CONFIRMATION_LINK, "MessageTemplateConsumer.EmailConfirmation.var.confirmationLink", true));
		return vars;
	}

	@Override
	public Set<String> getCompatibleFacilities()
	{
		return Collections.unmodifiableSet(Sets
				.newHashSet(FacilityName.EMAIL.toString()));
	}
}
