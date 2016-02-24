/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.server.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.server.translation.form.action.SubmitMessageActionFactory;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public abstract class BaseRegistrationTranslationProfile extends TranslationProfileInstance
						<RegistrationTranslationAction, RegistrationTranslationRule>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BaseRegistrationTranslationProfile.class);
	
	public BaseRegistrationTranslationProfile(ObjectNode json, RegistrationActionsRegistry registry)
	{
		super(json, registry);
	}
	
	public BaseRegistrationTranslationProfile(String name, List<? extends TranslationRule> rules, 
			TypesRegistryBase<? extends TranslationActionFactory> registry)
	{
		super(name, "", ProfileType.REGISTRATION, rules, registry);
	}
	
	public TranslatedRegistrationRequest translate(BaseForm form, 
			UserRequestState<? extends BaseRegistrationInput> request) 
			throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), 
				RequestSubmitStatus.submitted, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId());
		return executeFilteredActions(form, request.getRequest(), mvelCtx, null);
	}
	
	public AutomaticRequestAction getAutoProcessAction(BaseForm form, 
			UserRequestState<? extends BaseRegistrationInput> request, RequestSubmitStatus status)
	{
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), status, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId());
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, 
					request.getRequest(), mvelCtx, AutoProcessActionFactory.NAME);
		} catch (EngineException e)
		{
			log.error("Couldn't establish automatic request processing action from profile", e);
			return null;
		}
		return result.getAutoAction();
	}

	public I18nMessage getPostSubmitMessage(BaseForm form, BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request, RequestSubmitStatus.submitted, 
				context.triggeringMode, context.isOnIdpEndpoint, requestId);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, request, mvelCtx, SubmitMessageActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish post submission message from profile", e);
			return null;
		}
		return result.getPostSubmitMessage();
	}
	
	public String getPostSubmitRedirectURL(BaseForm form, BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request, RequestSubmitStatus.submitted, 
				context.triggeringMode, context.isOnIdpEndpoint, requestId);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, request, mvelCtx, RedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish redirect URL from profile", e);
			return null;
		}
		return result.getRedirectURL();
	}

	public String getPostCancelledRedirectURL(BaseForm form, RegistrationContext context)
	{
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, RequestSubmitStatus.notSubmitted, 
				context.triggeringMode, context.isOnIdpEndpoint);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, null, mvelCtx, RedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish redirect URL from profile", e);
			return null;
		}
		return result.getRedirectURL();
	}

	protected TranslatedRegistrationRequest executeFilteredActions(BaseForm form, 
			BaseRegistrationInput request, Map<String, Object> mvelCtx, 
			String actionNameFilter) throws EngineException
	{
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from registration request:\n" + mvelCtx);
		try
		{
			int i=1;
			TranslatedRegistrationRequest translationState = initializeTranslationResult(form, request);
			for (RegistrationTranslationRule rule: ruleInstances)
			{
				String actionName = rule.getAction().getName();
				if (actionNameFilter != null && !actionNameFilter.equals(actionName))
					continue;
				NDC.push("[r: " + (i++) + "]");
				try
				{
					rule.invoke(translationState, mvelCtx, getName());
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
	
	protected TranslatedRegistrationRequest initializeTranslationResult(BaseForm form, 
			BaseRegistrationInput request)
	{
		TranslatedRegistrationRequest initial = new TranslatedRegistrationRequest();
		if (request == null)
			return initial;
		
		request.getAttributes().stream().
			filter(a -> a != null).
			forEach(a -> initial.addAttribute(a));
		request.getIdentities().stream().
			filter(i -> i != null).
			forEach(i -> initial.addIdentity(i));
		for (int i = 0; i<request.getGroupSelections().size(); i++)
		{
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(i);
			Selection selection = request.getGroupSelections().get(i);
			if (selection != null && selection.isSelected())
				initial.addMembership(new GroupParam(groupRegistrationParam.getGroupPath(), 
					selection.getExternalIdp(), selection.getTranslationProfile()));			
		}
		
		return initial;
	}
	
	@Override
	protected RegistrationTranslationRule createRule(TranslationActionInstance action,
			TranslationCondition condition)
	{
		if (!(action instanceof RegistrationTranslationAction))
		{
			throw new InternalException("The translation action of the input translation "
					+ "profile is not compatible with it, it is " + action.getClass());
		}
		return new RegistrationTranslationRule((RegistrationTranslationAction) action, condition);
	}
}
