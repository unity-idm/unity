/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.ConfirmationRedirectActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.SubmitMessageActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public abstract class BaseFormTranslationProfile extends TranslationProfileInstance
						<RegistrationTranslationAction, RegistrationTranslationRule>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BaseFormTranslationProfile.class);
	private AttributeTypeHelper atHelper;
	
	public BaseFormTranslationProfile(TranslationProfile profile, RegistrationActionsRegistry registry, 
			AttributeTypeHelper atHelper)
	{
		super(profile, registry);
		this.atHelper = atHelper;
	}
	
	public TranslatedRegistrationRequest translate(BaseForm form, 
			UserRequestState<? extends BaseRegistrationInput> request) 
			throws EngineException
	{
		NDC.push("[TrProfile " + profile.getName() + "]");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), 
				RequestSubmitStatus.submitted, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId(), atHelper);
		return executeFilteredActions(form, request.getRequest(), mvelCtx, null);
	}
	
	public AutomaticRequestAction getAutoProcessAction(BaseForm form, 
			UserRequestState<? extends BaseRegistrationInput> request, RequestSubmitStatus status)
	{
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request.getRequest(), status, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint,
				request.getRequestId(), atHelper);
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
				context.triggeringMode, context.isOnIdpEndpoint, requestId, atHelper);
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
				context.triggeringMode, context.isOnIdpEndpoint, requestId, atHelper);
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

	public String getPostConfirmationRedirectURL(BaseForm form, UserRequestState<?> request,
			IdentityParam confirmed, String requestId)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				requestId,
				ConfirmationRedirectURLBuilder.ConfirmedElementType.identity.toString(), 
				confirmed.getTypeId(), confirmed.getValue());
	}

	public String getPostConfirmationRedirectURL(BaseForm form, UserRequestState<?> request,
			Attribute confirmed, String requestId)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				requestId,
				ConfirmationRedirectURLBuilder.ConfirmedElementType.attribute.toString(), 
				confirmed.getName(), confirmed.getValues().get(0).toString());
	}
	
	private String getPostConfirmationRedirectURL(BaseForm form, BaseRegistrationInput request,
			RegistrationContext regContxt, String requestId, 
			String cType, String cName, String cValue)
	{
		RegistrationMVELContext mvelCtx = new RegistrationMVELContext(form, request, 
				RequestSubmitStatus.submitted, 
				regContxt.triggeringMode, regContxt.isOnIdpEndpoint, requestId, atHelper);
		mvelCtx.addConfirmationContext(cType, cName, cValue);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(form, 
					request, mvelCtx, ConfirmationRedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.error("Couldn't establish redirect URL from profile", e);
			return null;
		}
		
		return "".equals(result.getRedirectURL()) ? null : result.getRedirectURL();
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
