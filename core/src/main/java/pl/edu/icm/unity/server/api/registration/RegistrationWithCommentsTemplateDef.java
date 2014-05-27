/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.api.registration;

import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;

/**
 * Base class for all {@link MessageTemplateDefinition}s of the registration forms subsystem which
 * can contain comments.
 *   
 * @author P. Piernik
 */
@Component
public abstract class RegistrationWithCommentsTemplateDef extends BaseRegistrationTemplateDef
{
	public static final String PUBLIC_COMMENT = "publicComment";
	public static final String INTERNAL_COMMENT = "internalComment";
	
	public RegistrationWithCommentsTemplateDef(String name, String descriptionKey)
	{
		super(name, descriptionKey);
	}

	@Override
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = super.getVariables();
		vars.put(PUBLIC_COMMENT, "MessageTemplateConsumer.BaseForm.var.publicComment");
		vars.put(INTERNAL_COMMENT, "MessageTemplateConsumer.BaseForm.var.internalComment");
		return vars;
	}
}
