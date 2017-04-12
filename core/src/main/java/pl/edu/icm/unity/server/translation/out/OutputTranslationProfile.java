/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationRule;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entry point: output translation profile, a list of translation rules
 * annotated with a name and description.
 * 
 * @author K. Benedyczak
 */
public class OutputTranslationProfile
		extends TranslationProfileInstance<OutputTranslationAction, OutputTranslationRule>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
			OutputTranslationProfile.class);

	public OutputTranslationProfile(String name, String description,
			List<? extends TranslationRule> rules,
			OutputTranslationActionsRegistry registry)
	{
		super(name, description, ProfileType.OUTPUT, rules, registry);
	}

	public OutputTranslationProfile(String name, List<? extends TranslationRule> rules,
			OutputTranslationActionsRegistry registry)
	{
		super(name, "", ProfileType.OUTPUT, rules, registry);
	}

	public OutputTranslationProfile(ObjectNode json, OutputTranslationActionsRegistry registry)
	{
		super(json, registry);
	}

	public TranslationResult translate(TranslationInput input) throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from local database:\n" + input.getTextDump());
		Object mvelCtx = createMvelContext(input);
		try
		{
			int i = 1;
			TranslationResult translationState = initiateTranslationResult(input);
			for (OutputTranslationRule rule : ruleInstances)
			{
				NDC.push("[r: " + (i++) + "]");
				try
				{
					rule.invoke(input, mvelCtx, getName(), translationState);
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

	public static Map<String, Object> createMvelContext(TranslationInput input)
	{
		Map<String, Object> ret = new HashMap<>();

		ret.put("protocol", input.getProtocol());
		ret.put("protocolSubtype", input.getProtocolSubType());
		ret.put("requester", input.getRequester());
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, List<? extends Object>> attrs = new HashMap<String, List<?>>();
		for (Attribute<?> ra : input.getAttributes())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put("attr", attr);
		ret.put("attrs", attrs);

		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (Identity id : input.getEntity().getIdentities())
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<String>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		ret.put("idsByType", idsByType);

		ret.put("groups", new ArrayList<String>(input.getGroups()));

		ret.put("usedGroup", input.getChosenGroup());

		Group main = new Group(input.getChosenGroup());
		List<String> subgroups = new ArrayList<String>();
		for (String group : input.getGroups())
		{
			Group g = new Group(group);
			if (g.isChild(main))
				subgroups.add(group);
		}
		ret.put("subGroups", subgroups);

		if (InvocationContext.hasCurrent())
		{
			LoginSession loginSession = InvocationContext.getCurrent()
					.getLoginSession();
			Set<String> authenticatedIdentities = loginSession
					.getAuthenticatedIdentities();
			ret.put("authenticatedWith",
					new ArrayList<String>(authenticatedIdentities));
			ret.put("idp", loginSession.getRemoteIdP() == null ? "_LOCAL"
					: loginSession.getRemoteIdP());
		} else
		{
			ret.put("authenticatedWith", new ArrayList<String>());
			ret.put("idp", null);
		}
		return ret;
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
}
