/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.engine.api.group.GroupsChain;
import pl.edu.icm.unity.engine.api.mvel.MVELGroup;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.TranslationRuleInvocationContext;

/**
 * Entry point: output translation profile, a list of translation rules
 * annotated with a name and description.
 * 
 * @author K. Benedyczak
 */
public class OutputTranslationProfile
		extends TranslationProfileInstance<OutputTranslationAction, OutputTranslationRule>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, OutputTranslationProfile.class);
	
	private OutputTranslationActionsRegistry registry;
	private OutputTranslationProfileRepository profileRepo;
	private GroupsManagement groupMan;
	private AttributeValueConverter attrConverter;
	
	public OutputTranslationProfile(TranslationProfile profile, OutputTranslationProfileRepository profileRepo,
			OutputTranslationActionsRegistry registry, AttributeValueConverter attrConverter,
			GroupsManagement groupProvider)
	{
		super(profile, registry);
		this.registry = registry;
		this.profileRepo = profileRepo;
		this.attrConverter = attrConverter;
		this.groupMan = groupProvider;
	}
	
	public TranslationResult translate(TranslationInput input) throws EngineException
	{
		return translate(input, null);
	}

	private TranslationResult translate(TranslationInput input, TranslationResult partialState) throws EngineException
	{
		NDC.push("[TrProfile " + profile.getName() + "]");
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from local database:\n" + input.getTextDump());
		Object mvelCtx = createMvelContext(input, attrConverter, g -> getGroupChain(g));
		try
		{
			int i = 1;
			TranslationResult translationState = null;
			if (partialState == null)
				translationState = initiateTranslationResult(input);
			else
				translationState = partialState;
				
			for (OutputTranslationRule rule : ruleInstances)
			{
				NDC.push("[r: " + (i++) + " " + rule.getAction().getName() + "]");
				try
				{
					TranslationRuleInvocationContext context = rule.invoke(
							input, mvelCtx, profile.getName(),
							translationState);
					if (context.getIncludedProfile() != null)
					{
						invokeOutputTranslationProfile(
								context.getIncludedProfile(), input,
								translationState);
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

	public static TranslationResult initiateTranslationResult(TranslationInput input)
	{
		TranslationResult ret = new TranslationResult();
		Collection<DynamicAttribute> attrs = input.getAttributes().stream()
				.map(attr -> new DynamicAttribute(attr))
				.collect(Collectors.toList());
		ret.getAttributes().addAll(attrs);
		for (Identity id : input.getEntity().getIdentities())
			ret.getIdentities().add(id);
		return ret;
	}

	static Map<String, Object> createMvelContext(TranslationInput input, 
			AttributeValueConverter attrConverter, Function<String, GroupsChain> groupProvider) throws IllegalAttributeValueException
	{
		Map<String, Object> ret = new HashMap<>();

		ret.put(OutputTranslationMVELContextKey.protocol.name(), input.getProtocol());
		ret.put(OutputTranslationMVELContextKey.protocolSubtype.name(), input.getProtocolSubType());
		ret.put(OutputTranslationMVELContextKey.requester.name(), input.getRequester());
		
		addAttributesToContext(OutputTranslationMVELContextKey.attr.name(),
				OutputTranslationMVELContextKey.attrObj.name(), OutputTranslationMVELContextKey.attrs.name(), ret,
				input.getAttributes(), attrConverter);
		addAttributesToContext(OutputTranslationMVELContextKey.requesterAttr.name(),
				OutputTranslationMVELContextKey.requesterAttrObj.name(),
				OutputTranslationMVELContextKey.requesterAttrs.name(), ret, input.getRequesterAttributes(),
				attrConverter);

		Map<String, List<String>> idsByType = new HashMap<>();
		for (Identity id : input.getEntity().getIdentities())
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		ret.put(OutputTranslationMVELContextKey.idsByType.name(), idsByType);

		ret.put(OutputTranslationMVELContextKey.importStatus.name(), input.getImportStatus().entrySet().stream()
		                  .collect(Collectors.toMap(Entry::getKey, e -> String.valueOf(e.getValue()))));
		
		List<String> groupNames = input.getGroups().stream()
				.map(group -> group.getName())
				.collect(Collectors.toList());
		
		ret.put(OutputTranslationMVELContextKey.groups.name(), groupNames);

		ret.put(OutputTranslationMVELContextKey.usedGroup.name(), input.getChosenGroup());

		Group main = new Group(input.getChosenGroup());
		List<String> subgroups = new ArrayList<String>();
		for (String group : groupNames)
		{
			Group g = new Group(group);
			if (g.isChild(main))
				subgroups.add(group);
		}
		ret.put(OutputTranslationMVELContextKey.subGroups.name(), subgroups);

		
		Map<String, MVELGroup> groupsObj = input.getGroups().stream()
				.collect(Collectors.toMap(group -> group.getName(), 
						group -> new MVELGroup(groupProvider.apply(group.getPathEncoded()))));
		ret.put(OutputTranslationMVELContextKey.groupsObj.name(), groupsObj);
		
		if (InvocationContext.hasCurrent() && InvocationContext.getCurrent().getLoginSession() != null)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();

			Set<String> authenticatedIdentities = loginSession.getAuthenticatedIdentities();
			ret.put(OutputTranslationMVELContextKey.authenticatedWith.name(),
					new ArrayList<String>(authenticatedIdentities));
			ret.put(OutputTranslationMVELContextKey.idp.name(),
					loginSession.getRemoteIdP() == null ? "_LOCAL" : loginSession.getRemoteIdP());
			List<String> usedAuthenticators = new ArrayList<>();
			if (loginSession.getLogin1stFactor().optionId != null)
				usedAuthenticators.add(loginSession.getLogin1stFactor().optionId.getAuthenticatorKey());
			if (loginSession.getLogin2ndFactor().optionId != null)
				usedAuthenticators.add(loginSession.getLogin2ndFactor().optionId.getAuthenticatorKey());
			ret.put(OutputTranslationMVELContextKey.authentications.name(), usedAuthenticators);
			ret.put(OutputTranslationMVELContextKey.mfa.name(), usedAuthenticators.size() > 1);
			ret.put(OutputTranslationMVELContextKey.twoStepAuthn.name(), usedAuthenticators.size() > 1);
			ret.putAll(getAuthnContextMvelVariables(loginSession.getFirstFactorRemoteIdPAuthnContext()));
			ret.put(OutputTranslationMVELContextKey.amr.name(), AuthenticationMethodsToMvelContextMapper
					.getAuthenticationMethodsWithMFAandMCAIfUsed(loginSession.getAuthenticationMethods()));
			
		} else
		{
			ret.put(OutputTranslationMVELContextKey.authenticatedWith.name(), new ArrayList<String>());
			ret.put(OutputTranslationMVELContextKey.idp.name(), null);
		}
		return ret;
	}
	
	private static Map<String, Object> getAuthnContextMvelVariables(RemoteAuthnMetadata authnContext)
	{
		Map<String, Object> ret = new HashMap<>();
		
		List<String> acrs = new ArrayList<>();
		String upstreamProtocol = OutputTranslationMVELContextKey.DEFAULT_UPSTREAM_PROTOCOL;
		String upstreamIdP = null;
		
		if (authnContext != null)
		{
			acrs.addAll(authnContext.classReferences());
			upstreamIdP = authnContext.remoteIdPId();
			upstreamProtocol = authnContext.protocol().name();
		}
		
		ret.put(OutputTranslationMVELContextKey.upstreamACRs.name(), acrs);
		ret.put(OutputTranslationMVELContextKey.upstreamProtocol.name(), upstreamProtocol);
		ret.put(OutputTranslationMVELContextKey.upstreamIdP.name(), upstreamIdP);
		return ret;
	}

	private static void addAttributesToContext(String attrKey, String attrObjKey, String attrsKey, Map<String, Object> ret, 
			Collection<Attribute> attributes, AttributeValueConverter attrConverter) 
					throws IllegalAttributeValueException
	{
		Map<String, Object> attr = new HashMap<>();
		Map<String, Object> attrObj = new HashMap<>();
		Map<String, List<? extends Object>> attrs = new HashMap<>();
		
		for (Attribute ra: attributes)
		{
			List<String> values = attrConverter.internalValuesToExternal(ra.getName(),
					ra.getValues());
			String v = values.isEmpty() ? "" : values.get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), values);
			attrObj.put(ra.getName(), values.isEmpty() ? ""
					: attrConverter.internalValuesToObjectValues(ra.getName(),
							ra.getValues()));
		}
		ret.put(attrKey, attr);
		ret.put(attrObjKey, attrObj);
		ret.put(attrsKey, attrs);
	}
	
	@Override
	protected OutputTranslationRule createRule(TranslationActionInstance action,
			TranslationCondition condition)
	{
		if (!(action instanceof OutputTranslationAction))
		{
			throw new InternalException(
					"The translation action of the input translation "
							+ "profile is not compatible with it, it is "
							+ action.getClass());
		}
		return new OutputTranslationRule((OutputTranslationAction) action, condition);
	}
	
	private TranslationResult invokeOutputTranslationProfile(String profile,
			TranslationInput input, TranslationResult translationState)
			throws EngineException
	{
		TranslationProfile translationProfile = profileRepo.listAllProfiles()
				.get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The output translation profile '"
					+ profile + "' included in another profile does not exist");
		OutputTranslationProfile profileInstance = new OutputTranslationProfile( 
				translationProfile, profileRepo, registry, attrConverter, groupMan);
		TranslationResult result = profileInstance.translate(input, translationState);
		return result;
	}
	
	
	
	private GroupsChain getGroupChain(String g) 
	{
		try
		{
			return groupMan.getGroupsChain(g);
		} catch (EngineException e)
		{
			log.error("Can not get group", e);
			throw new RuntimeEngineException(e);
		}		
	}
}
