/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.types.basic.AttributeVisibility;


/**
 * Simplifies creation of typical {@link RegistrationTranslationProfile}s.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfileBuilder
{
	private List<RegistrationTranslationRule> rules = new ArrayList<>();
	private String name;
	private TranslationActionsRegistry registry;
	

	public RegistrationTranslationProfileBuilder(TranslationActionsRegistry registry, String name)
	{
		this.registry = registry;
		this.name = name;
	}
	
	public RegistrationTranslationProfileBuilder withAutoProcess(String condition, 
			AutomaticRequestAction action)
	{
		return withRule(AutoProcessActionFactory.NAME, condition, action.toString());
	}

	public RegistrationTranslationProfileBuilder withAddAttribute(String condition, 
			String attribute, String group, String value, AttributeVisibility visibility)
	{
		return withRule(AddAttributeActionFactory.NAME, condition, attribute, group, 
				value, visibility.toString());
	}

	public RegistrationTranslationProfileBuilder withRule(String ruleName, String condition, 
			String... parameters)
	{
		RegistrationTranslationAction action;
		try
		{
			action = (RegistrationTranslationAction) registry.getByName(
					ruleName).getInstance(parameters);
		} catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
		rules.add(new RegistrationTranslationRule(
				action, 
				new TranslationCondition(condition)));
		
		return this;
	}

	public RegistrationTranslationProfile build()
	{
		return new RegistrationTranslationProfile(name, rules);
	}
}
