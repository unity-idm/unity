/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Entry point: input translation profile, a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class InputTranslationProfile extends AbstractTranslationProfile<InputTranslationRule>
{
	public enum ContextKey
	{
		id,
		idType,
		idsByType,
		attrs,
		attr,
		idp,
		groups;
	}

	
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationProfile.class);
	
	public InputTranslationProfile(String name, List<InputTranslationRule> rules)
	{
		super(name, ProfileType.INPUT, rules);
	}

	public InputTranslationProfile(String json, ObjectMapper jsonMapper, TranslationActionsRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}
	
	public MappingResult translate(RemotelyAuthenticatedInput input) throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		if (log.isDebugEnabled())
			log.debug("Input received from IdP " + input.getIdpName() + ":\n" + input.getTextDump());
		Object mvelCtx = createMvelContext(input);
		try
		{
			int i=1;
			MappingResult translationState = new MappingResult();
			for (InputTranslationRule rule: rules)
			{
				NDC.push("[r: " + (i++) + "]");
				try
				{
					rule.invoke(input, mvelCtx, translationState, getName());
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
	
	public static Map<String, Object> createMvelContext(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> ret = new HashMap<>();
		
		ret.put(ContextKey.idp.name(), input.getIdpName());
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, List<Object>> attrs = new HashMap<String, List<Object>>();
		for (RemoteAttribute ra: input.getAttributes().values())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put(ContextKey.attr.name(), attr);
		ret.put(ContextKey.attrs.name(), attrs);
		
		if (!input.getIdentities().isEmpty())
		{
			RemoteIdentity ri = input.getIdentities().values().iterator().next();
			ret.put(ContextKey.id.name(), ri.getName());
			ret.put(ContextKey.idType.name(), ri.getIdentityType());
		}
		
		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (RemoteIdentity ri: input.getIdentities().values())
		{
			List<String> vals = idsByType.get(ri.getIdentityType());
			if (vals == null)
			{
				vals = new ArrayList<String>();
				idsByType.put(ri.getIdentityType(), vals);
			}
			vals.add(ri.getName());
		}
		ret.put(ContextKey.idsByType.name(), idsByType);
		
		ret.put(ContextKey.groups.name(), new ArrayList<String>(input.getGroups().keySet()));
		return ret;
	}
	
	public static Map<String, String> createExpresionValueMap(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> mvelCtx = createMvelContext(input);
		return createExpresionValueMap(mvelCtx);
	}
	
	public static Map<String, String> createExpresionValueMap(Map<String, Object> mvelCtx)
	{
		Map<String, String> exprValMap = new LinkedHashMap<String, String>();

		for (Map.Entry<String, Object> context : mvelCtx.entrySet())
		{
			String contextKey = context.getKey();
			Object contextValue = context.getValue();
			try
			{
				ContextKey.valueOf(contextKey);
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Incorrect MVEL context, unknown context key: " + 
						context.getKey());
			}
			
			if (contextValue instanceof Map)
			{
				@SuppressWarnings("unchecked")
				HashMap<String, Object> value = (HashMap<String, Object>) contextValue;
				for (Map.Entry<String, Object> entry : value.entrySet())
				{
					exprValMap.put(String.format("%s['%s']", contextKey, entry.getKey()), 
							entry.getValue().toString());
				}
			} else if (contextValue instanceof List)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else if (contextValue instanceof String)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else
			{
				throw new IllegalArgumentException("Incorrect MVEL context: unexpected: \"" 
						+ contextValue.getClass() 
						+ "\" type for context key: \"" 
						+ contextKey 
						+ "\"");
			}
		}
		
		return exprValMap;
	}
	
	@Override
	protected InputTranslationRule createRule(TranslationAction action, TranslationCondition condition)
	{
		if (!(action instanceof InputTranslationAction))
		{
			throw new InternalException("The translation action of the input translation "
					+ "profile is not compatible with it, it is " + action.getClass());
		}
		
		return new InputTranslationRule((InputTranslationAction) action, condition);
	}
}
