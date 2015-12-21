/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.RegistrationContext;
import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.registries.RegistrationTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.server.translation.form.action.ConfirmationRedirectActionFactory;
import pl.edu.icm.unity.server.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.Selection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfile extends AbstractTranslationProfile<RegistrationTranslationRule>
{
	public enum RequestSubmitStatus 
	{
		submitted,
		notSubmitted
	}
	
	public enum ContextKey
	{
		idsByType,
		ridsByType,
		idsByTypeObj,
		ridsByTypeObj,
		attrs,
		attr,
		rattrs,
		rattr,
		groups,
		rgroups,
		status,
		triggered,
		onIdpEndpoint,
		userLocale,
		registrationForm,
		requestId,
		agrs;
	}
	
	public enum PostConfirmationContextKey
	{
		confirmedElementType,
		confirmedElementName,
		confirmedElementValue
	}
	

	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RegistrationTranslationProfile.class);
	
	public RegistrationTranslationProfile(ObjectNode json, RegistrationTranslationActionsRegistry registry)
	{
		fromJson(json, registry);
	}
	
	public RegistrationTranslationProfile(String json, ObjectMapper jsonMapper, 
			RegistrationTranslationActionsRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}
	
	public RegistrationTranslationProfile(String name, List<RegistrationTranslationRule> rules)
	{
		super(name, ProfileType.REGISTRATION, rules);
	}
	
	public TranslatedRegistrationRequest translate(RegistrationForm form, RegistrationRequestState request) 
			throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		Map<String, Object> mvelCtx = createMvelContext(form, request.getRequest(), 
				RequestSubmitStatus.submitted, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint);
		return executeFilteredActions(form, request.getRequest(), mvelCtx, null);
	}
	
	public AutomaticRequestAction getAutoProcessAction(RegistrationForm form, 
			RegistrationRequestState request, RequestSubmitStatus status)
	{
		Map<String, Object> mvelCtx = createMvelContext(form, request.getRequest(), status, 
				request.getRegistrationContext().triggeringMode, 
				request.getRegistrationContext().isOnIdpEndpoint);
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
	
	public String getPostSubmitRedirectURL(RegistrationForm form, RegistrationRequest request,
			RegistrationContext context)
	{
		Map<String, Object> mvelCtx = createMvelContext(form, request, RequestSubmitStatus.submitted, 
				context.triggeringMode, context.isOnIdpEndpoint);
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

	public String getPostCancelledRedirectURL(RegistrationForm form, RegistrationContext context)
	{
		Map<String, Object> mvelCtx = createBaseMvelContext(form, RequestSubmitStatus.notSubmitted, 
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

	public String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequestState request,
			IdentityParam confirmed)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				ConfirmationRedirectURLBuilder.ConfirmedElementType.identity.toString(), 
				confirmed.getTypeId(), confirmed.getValue());
	}

	public String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequestState request,
			Attribute<?> confirmed)
	{
		return getPostConfirmationRedirectURL(form, request.getRequest(), request.getRegistrationContext(),
				ConfirmationRedirectURLBuilder.ConfirmedElementType.attribute.toString(), 
				confirmed.getName(), confirmed.getValues().get(0).toString());
	}
	
	private String getPostConfirmationRedirectURL(RegistrationForm form, RegistrationRequest request,
			RegistrationContext regContxt, 
			String cType, String cName, String cValue)
	{
		Map<String, Object> mvelCtx = createMvelContext(form, request, RequestSubmitStatus.submitted, 
				regContxt.triggeringMode, regContxt.isOnIdpEndpoint);
		addConfirmationContext(mvelCtx, cType, cName, cValue);
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
	
	private TranslatedRegistrationRequest executeFilteredActions(RegistrationForm form, 
			RegistrationRequest request, Map<String, Object> mvelCtx, 
			String actionNameFilter) throws EngineException
	{
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from registration request:\n" + contextToString(mvelCtx));
		try
		{
			int i=1;
			TranslatedRegistrationRequest translationState = request == null ?
					new TranslatedRegistrationRequest(form.getDefaultCredentialRequirement()) : 
					initializeTranslationResult(form, request);
			for (RegistrationTranslationRule rule: rules)
			{
				String actionName = rule.getAction().getActionDescription().getName();
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
	
	private TranslatedRegistrationRequest initializeTranslationResult(RegistrationForm form, 
			RegistrationRequest request)
	{
		TranslatedRegistrationRequest initial = new TranslatedRegistrationRequest(
				form.getDefaultCredentialRequirement());

		request.getAttributes().
			forEach(a -> initial.addAttribute(a));
		request.getIdentities().
			forEach(i -> initial.addIdentity(i));
		for (int i = 0; i<request.getGroupSelections().size(); i++)
		{
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(i);
			Selection selection = request.getGroupSelections().get(i);
			if (selection.isSelected())
				initial.addMembership(new GroupParam(groupRegistrationParam.getGroupPath(), 
					selection.getExternalIdp(), selection.getTranslationProfile()));			
		}
		
		return initial;
	}
	
	@Override
	protected RegistrationTranslationRule createRule(TranslationAction action,
			TranslationCondition condition)
	{
		if (!(action instanceof RegistrationTranslationAction))
		{
			throw new InternalException("The translation action of the input translation "
					+ "profile is not compatible with it, it is " + action.getClass());
		}
		return new RegistrationTranslationRule((RegistrationTranslationAction) action, condition);
	}
	
	
	public static void addConfirmationContext(Map<String, Object> ctx, String confirmedElementType, 
			String confirmedElementName, String confirmedElementValue)
	{
		ctx.put(PostConfirmationContextKey.confirmedElementName.toString(), confirmedElementName);
		ctx.put(PostConfirmationContextKey.confirmedElementValue.toString(), confirmedElementValue);
		ctx.put(PostConfirmationContextKey.confirmedElementType.toString(), confirmedElementType);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> createMvelContext(RegistrationForm form, RegistrationRequest request,
			RequestSubmitStatus status, TriggeringMode triggered, boolean idpEndpoint)
	{
		Map<String, Object> ret = createBaseMvelContext(form, status, triggered, idpEndpoint);
		
		ret.put(ContextKey.userLocale.name(), request.getUserLocale());
		
		Map<String, Object> attr = new HashMap<>();
		Map<String, List<Object>> attrs = new HashMap<>();
		Map<String, Object> rattr = new HashMap<>();
		Map<String, List<Object>> rattrs = new HashMap<>();
		
		for (int i=0; i<request.getAttributes().size(); i++)
		{
			AttributeRegistrationParam attributeRegistrationParam = form.getAttributeParams().get(i);
			Attribute<?> attribute = request.getAttributes().get(i);
			Object v = attribute.getValues().isEmpty() ? "" : attribute.getValues().get(0);
			attr.put(attribute.getName(), v);
			attrs.put(attribute.getName(), (List<Object>) attribute.getValues());

			if (attributeRegistrationParam.getRetrievalSettings().isAutomaticOnly())
			{
				rattr.put(attribute.getName(), v);
				rattrs.put(attribute.getName(), (List<Object>) attribute.getValues());				
			}
		}
		ret.put(ContextKey.attr.name(), attr);
		ret.put(ContextKey.attrs.name(), attrs);
		ret.put(ContextKey.rattr.name(), rattr);
		ret.put(ContextKey.rattrs.name(), rattrs);

		
		Map<String, List<String>> idsByType = new HashMap<>();
		Map<String, List<String>> ridsByType = new HashMap<>();
		Map<String, List<Object>> idsByTypeObj = new HashMap<>();
		Map<String, List<Object>> ridsByTypeObj = new HashMap<>();
		for (int i=0; i<request.getIdentities().size(); i++)
		{
			IdentityRegistrationParam identityRegistrationParam = form.getIdentityParams().get(i);
			IdentityParam identityParam = request.getIdentities().get(i);

			List<String> vals = idsByType.get(identityParam.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(identityParam.getTypeId(), vals);
			}
			vals.add(identityParam.getValue());

			List<Object> valsObj = idsByTypeObj.get(identityParam.getTypeId());
			if (valsObj == null)
			{
				valsObj = new ArrayList<>();
				idsByTypeObj.put(identityParam.getTypeId(), valsObj);
			}
			valsObj.add(identityParam.getValue());
			
			
			if (identityRegistrationParam.getRetrievalSettings().isAutomaticOnly())
			{
				List<String> rvals = ridsByType.get(identityParam.getTypeId());
				if (rvals == null)
				{
					rvals = new ArrayList<>();
					ridsByType.put(identityParam.getTypeId(), rvals);
				}
				rvals.add(identityParam.getValue());			

				List<Object> rvalsObj = ridsByTypeObj.get(identityParam.getTypeId());
				if (rvalsObj == null)
				{
					rvalsObj = new ArrayList<>();
					ridsByTypeObj.put(identityParam.getTypeId(), rvalsObj);
				}
				rvalsObj.add(identityParam.getValue());			
			}
		}

		ret.put(ContextKey.idsByType.name(), idsByType);
		ret.put(ContextKey.ridsByType.name(), ridsByType);
		ret.put(ContextKey.idsByTypeObj.name(), idsByTypeObj);
		ret.put(ContextKey.ridsByTypeObj.name(), ridsByTypeObj);
		
		List<String> groups = new ArrayList<>();
		List<String> rgroups = new ArrayList<>();
		for (int i=0; i<request.getGroupSelections().size(); i++)
		{
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(i);
			Selection selection = request.getGroupSelections().get(i);
			if (selection.isSelected())
			{
				groups.add(groupRegistrationParam.getGroupPath());
				if (groupRegistrationParam.getRetrievalSettings().isAutomaticOnly())
					rgroups.add(groupRegistrationParam.getGroupPath());
			}
		}
		ret.put(ContextKey.groups.name(), groups);
		ret.put(ContextKey.rgroups.name(), rgroups);
		
		ArrayList<String> agr = new ArrayList<String>();
		for (Selection a : request.getAgreements())
		{
			agr.add(Boolean.toString(a.isSelected()));
		}
		ret.put(ContextKey.agrs.name(), agr);
		return ret;
	}
	
	public static Map<String, Object> createBaseMvelContext(RegistrationForm form,
			RequestSubmitStatus status, TriggeringMode triggered, boolean idpEndpoint)
	{
		Map<String, Object> ret = new HashMap<>();
		ret.put(ContextKey.onIdpEndpoint.name(), idpEndpoint);
		ret.put(ContextKey.triggered.name(), triggered.toString());
		ret.put(ContextKey.status.name(), status.toString());
		return ret;
	}
	
	private String contextToString(Map<String, Object> context)
	{
		StringJoiner joiner = new StringJoiner("\n");
		context.forEach((key, value) -> joiner.add(key + " = " + value));
		return joiner.toString();
	}
	
	
}
