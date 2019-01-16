/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * The translation profile instance contains a {@link TranslationProfile}, enriched in that way that it can be executed,
 * while {@link TranslationProfile} only stores a description of a profile. This class is an abstract root of all 
 * types of translation profile instances, implementing initialization of {@link TranslationRuleInstance}s.
 * 
 * @author K. Benedyczak
 */
public abstract class TranslationProfileInstance<T extends TranslationActionInstance, 
			R extends TranslationRuleInstance<?>> 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TranslationActionInstance.class);
	protected List<R> ruleInstances;
	protected TranslationProfile profile;
	protected boolean hasInvalidActions;
	
	public TranslationProfileInstance(TranslationProfile profile, 
			TypesRegistryBase<? extends TranslationActionFactory<T>> registry)
	{
		this.profile = profile;
		initInstance(registry);
	}
	
	/**
	 * @return list of profile's rules
	 */
	public List<R> getRuleInstances()
	{
		return ruleInstances;
	}
	
	protected void initInstance(TypesRegistryBase<? extends TranslationActionFactory<T>> registry)
	{
		
		ruleInstances = new ArrayList<>(profile.getRules().size());
		try
		{
			for (TranslationRule rule: profile.getRules())
			{
				TranslationActionFactory<T> fact = registry.getByName(rule.getAction().getName());
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
	
	private TranslationActionInstance loadAction(TranslationActionFactory<T> fact, String[] parameters)
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
			hasInvalidActions = true;
			return fact.getBlindInstance(parameters);
		}
	}
	
	public TranslationProfile getProfile()
	{
		return profile;
	}

	/**
	 * 
	 * @return if true then some of the actions were not loaded properly.
	 */
	public boolean hasInvalidActions()
	{
		return hasInvalidActions;
	}

	/**
	 * Must return a correct instance of a rule. Should check if the action is of proper type.
	 * @param action
	 * @param condition
	 * @return
	 */
	protected abstract R createRule(TranslationActionInstance action, TranslationCondition condition);
}
