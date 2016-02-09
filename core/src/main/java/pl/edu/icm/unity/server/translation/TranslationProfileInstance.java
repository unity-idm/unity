/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The translation profile instance is a {@link TranslationProfile}, enriched in that way that it can be executed,
 * while {@link TranslationProfile} only stores a description of a profile. This class is an abstract root of all 
 * types of translation profile instances, implementing initialization of {@link TranslationRuleInstance}s.
 * 
 * @author K. Benedyczak
 */
public abstract class TranslationProfileInstance<T extends TranslationActionInstance, R extends TranslationRuleInstance<T>> 
	extends TranslationProfile
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TranslationActionInstance.class);
	protected List<R> ruleInstances;
	
	public TranslationProfileInstance(String name, String description, ProfileType profileType, 
			List<? extends TranslationRule> rules, TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(name, description, profileType, rules);
		initInstance(registry);
	}
	
	public TranslationProfileInstance(ObjectNode json, TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(json);
		initInstance(registry);
	}

	/**
	 * @return list of profile's rules
	 */
	public List<R> getRuleInstances()
	{
		return ruleInstances;
	}
	
	protected void initInstance(TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		
		ruleInstances = new ArrayList<>(getRules().size());
		try
		{
			for (TranslationRule rule: getRules())
			{
				TranslationActionFactory fact = registry.getByName(rule.getAction().getName());
				String[] parameters = rule.getAction().getParameters();
				TranslationActionInstance actionInstance = loadAction(fact, parameters);
				ruleInstances.add(createRule(actionInstance, 
						new TranslationCondition(rule.getCondition())));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't create runtime instance of a translation profile", e);
		}
	}
	
	private TranslationActionInstance loadAction(TranslationActionFactory fact, String[] parameters)
	{
		try
		{
			return fact.getInstance(parameters);
		} catch (Exception e)
		{
			log.error("Can not load action " + fact.getActionType().getName() + " with parameters: " +
					Arrays.toString(parameters) + 
					". This action will be ignored during profile's execution. "
					+ "Fix the action definition. This problem can occur after system "
					+ "reconfiguration when action definition becomes obsolete "
					+ "(e.g. using not existing attribute)", e);
			return fact.getBlindInstance(parameters);
		}
	}
	
	/**
	 * Must return a correct instance of a rule. Should check if the action is of proper type.
	 * @param action
	 * @param condition
	 * @return
	 */
	protected abstract R createRule(TranslationActionInstance action, TranslationCondition condition);
}
