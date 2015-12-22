/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.RegistrationTranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AddAttributeClassActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.server.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.server.translation.form.action.SetEntityStateActionFactory;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;


/**
 * Simplifies creation of typical {@link RegistrationTranslationProfile}s.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfileBuilder
{
	private List<RegistrationTranslationRule> rules = new ArrayList<>();
	private String name;
	private TypesRegistryBase<RegistrationTranslationActionFactory> registry;
	

	public RegistrationTranslationProfileBuilder(TypesRegistryBase<RegistrationTranslationActionFactory> registry,
			String name)
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

	public RegistrationTranslationProfileBuilder withGroupMembership(String condition, 
			String group)
	{
		return withRule(AddToGroupActionFactory.NAME, condition, group);
	}

	public RegistrationTranslationProfileBuilder withAttributeClass(String condition, 
			String group, String acExpression)
	{
		return withRule(AddAttributeClassActionFactory.NAME, condition, group, acExpression);
	}
	
	public RegistrationTranslationProfileBuilder withInitialState(String condition, 
			EntityState state)
	{
		return withRule(SetEntityStateActionFactory.NAME, condition, state.toString());
	}

	public RegistrationTranslationProfileBuilder withRedirect(String condition, 
			String redirectURL)
	{
		return withRule(RedirectActionFactory.NAME, condition, redirectURL);
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
