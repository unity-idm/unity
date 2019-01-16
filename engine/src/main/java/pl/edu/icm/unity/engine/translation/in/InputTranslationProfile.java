/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationContextFactory;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.TranslationRuleInvocationContext;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Entry point: input translation profile, a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class InputTranslationProfile extends TranslationProfileInstance<InputTranslationAction, InputTranslationRule>
{	
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationProfile.class);
	private InputTranslationActionsRegistry registry;
	private InputTranslationProfileRepository profileRepo;
	
	public InputTranslationProfile(TranslationProfile profile,
			InputTranslationProfileRepository profileRepo,
			InputTranslationActionsRegistry registry)
	{
		super(profile, registry);
		this.registry = registry;
		this.profileRepo = profileRepo;
	}	
	
	public MappingResult translate(RemotelyAuthenticatedInput input) throws EngineException
	{
		NDC.push("TrProfile " + profile.getName());
		if (log.isDebugEnabled())
			log.debug("Input received from IdP " + input.getIdpName() + ":\n"
					+ input.getTextDump());
		Object mvelCtx = InputTranslationContextFactory.createMvelContext(input);
		try
		{
			int i = 1;
			MappingResult translationState = new MappingResult();
			for (InputTranslationRule rule : ruleInstances)
			{
				NDC.push("r: " + (i++));
				try
				{
					TranslationRuleInvocationContext context = rule.invoke(
							input, mvelCtx, translationState,
							profile.getName());
					if (context.getIncludedProfile() != null)
					{
						MappingResult result = invokeInputTranslationProfile(
								context.getIncludedProfile(),
								input);
						translationState.mergeWith(result);
					}

				} catch (ExecutionBreakException e)
				{
					break;
				} finally
				{
					NDC.pop();
				}
			}
			return translationState;
		} finally
		{
			NDC.pop();
		}
	}
	
	
	@Override
	protected InputTranslationRule createRule(TranslationActionInstance action, TranslationCondition condition)
	{
		if (!(action instanceof InputTranslationAction))
		{
			throw new InternalException("The translation action of the input translation "
					+ "profile is not compatible with it, it is " + action.getClass());
		}
		
		return new InputTranslationRule((InputTranslationAction) action, condition);
	}
	
	private MappingResult invokeInputTranslationProfile(String profile, RemotelyAuthenticatedInput input) throws EngineException
	{
		TranslationProfile translationProfile = profileRepo.listAllProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The input translation profile '" + profile + 
					"' included in another profile does not exist");
		InputTranslationProfile profileInstance = new InputTranslationProfile(translationProfile, profileRepo,
				registry);
		MappingResult result = profileInstance.translate(input);
		return result;
	}

}	
