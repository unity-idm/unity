/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import java.util.Map;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public abstract class BaseFormTranslationProfile extends TranslationProfileInstance
						<RegistrationTranslationAction, RegistrationTranslationRule> 
				implements FormAutomationSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BaseFormTranslationProfile.class);
	private AttributeTypeHelper atHelper;
	protected BaseForm form;
	
	public BaseFormTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry, 
			AttributeTypeHelper atHelper, BaseForm form)
	{
		super(profile, registry);
		this.atHelper = atHelper;
		this.form = form;
	}
	
	public TranslatedRegistrationRequest translate(
			UserRequestState<? extends BaseRegistrationInput> request) 
			throws EngineException
	{
		log.debug("Executing form profile to postprocess the submitted data");
		NDC.push("[TrProfile " + profile.getName() + "]");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), 
				RequestSubmitStatus.submitted, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId(), atHelper);
		return executeFilteredActions(request.getRequest(), mvelCtx, null);
	}
	
	@Transactional
	@Override
	public AutomaticRequestAction getAutoProcessAction(
			UserRequestState<? extends BaseRegistrationInput> request, RequestSubmitStatus status)
	{
		log.debug("Consulting form profile to establish automatic processing action");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), status, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId(), atHelper);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(request.getRequest(), mvelCtx, AutoProcessActionFactory.NAME);
		} catch (EngineException e)
		{
			log.error("Couldn't establish automatic request processing action from profile", e);
			return null;
		}
		log.debug("Established automatic processing action: " + result.getAutoAction());
		return result.getAutoAction();
	}

	protected TranslatedRegistrationRequest executeFilteredActions(
			BaseRegistrationInput request, Map<String, Object> mvelCtx, 
			String actionNameFilter) throws EngineException
	{
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from registration request:\n" + mvelCtx);
		try
		{
			int i=1;
			TranslatedRegistrationRequest translationState = initializeTranslationResult(request);
			for (RegistrationTranslationRule rule: ruleInstances)
			{
				String actionName = rule.getAction().getName();
				if (actionNameFilter != null && !actionNameFilter.equals(actionName))
					continue;
				NDC.push("[r: " + (i++) + "]");
				try
				{
					rule.invoke(translationState, mvelCtx, profile.getName());
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
	
	protected TranslatedRegistrationRequest initializeTranslationResult(
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
		for (GroupSelection selection : request.getGroupSelections())
		{
			if (selection == null)
				continue;
			for (String group: selection.getSelectedGroups())
				initial.addMembership(new GroupParam(group, 
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
