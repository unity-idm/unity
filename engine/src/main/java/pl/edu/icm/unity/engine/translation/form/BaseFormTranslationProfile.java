/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.ConfirmationRedirectActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.SubmitMessageActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
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

	@Transactional
	@Override
	public I18nMessage getPostSubmitMessage(BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		log.debug("Consulting form profile to establish post-submit message");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request, RequestSubmitStatus.submitted, 
				context.triggeringMode, context.isOnIdpEndpoint, requestId, atHelper);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(request, mvelCtx, SubmitMessageActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish post submission message from profile", e);
			return null;
		}
		return result.getPostSubmitMessage();
	}
	
	@Transactional
	@Override
	public String getPostSubmitRedirectURL(BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		log.debug("Consulting form profile to establish post-submit redirect URL");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, request, RequestSubmitStatus.submitted, 
				context.triggeringMode, context.isOnIdpEndpoint, requestId, atHelper);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(request, mvelCtx, RedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish redirect URL from profile", e);
			return null;
		}
		return result.getRedirectURL();
	}

	@Transactional
	@Override
	public String getPostCancelledRedirectURL(RegistrationContext context)
	{
		log.debug("Consulting form profile to establish post-cancel redirect URL");
		Map<String, Object> mvelCtx = new RegistrationMVELContext(form, RequestSubmitStatus.notSubmitted, 
				context.triggeringMode, context.isOnIdpEndpoint);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(null, mvelCtx, RedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.warn("Couldn't establish redirect URL from profile", e);
			return null;
		}
		return result.getRedirectURL();
	}

	@Transactional
	@Override
	public String getPostConfirmationRedirectURL(UserRequestState<?> request,
			IdentityParam confirmed, String requestId)
	{
		return getPostConfirmationRedirectURL(request.getRequest(), request.getRegistrationContext(),
				requestId,
				EmailConfirmationRedirectURLBuilder.ConfirmedElementType.identity.toString(), 
				confirmed.getTypeId(), confirmed.getValue());
	}

	@Transactional
	@Override
	public String getPostConfirmationRedirectURL(UserRequestState<?> request,
			Attribute confirmed, String requestId)
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntaxForAttributeName(confirmed.getName());
		VerifiableElement parsed = (VerifiableElement) syntax.convertFromString(confirmed.getValues().get(0));
		return getPostConfirmationRedirectURL(request.getRequest(), request.getRegistrationContext(),
				requestId,
				EmailConfirmationRedirectURLBuilder.ConfirmedElementType.attribute.toString(), 
				confirmed.getName(), parsed.getValue());
	}
	
	private String getPostConfirmationRedirectURL(BaseRegistrationInput request,
			RegistrationContext regContxt, String requestId, 
			String cType, String cName, String cValue)
	{
		log.debug("Consulting form profile to establish post-confirmation redirect URL");
		RegistrationMVELContext mvelCtx = new RegistrationMVELContext(form, request, 
				RequestSubmitStatus.submitted, 
				regContxt.triggeringMode, regContxt.isOnIdpEndpoint, requestId, atHelper);
		mvelCtx.addConfirmationContext(cType, cName, cValue);
		TranslatedRegistrationRequest result;
		try
		{
			result = executeFilteredActions(request, mvelCtx, ConfirmationRedirectActionFactory.NAME);
		} catch (EngineException e)
		{
			log.error("Couldn't establish redirect URL from profile", e);
			return null;
		}
		
		return "".equals(result.getRedirectURL()) ? null : result.getRedirectURL();
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
