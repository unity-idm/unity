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
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entry point: input translation profile, a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class InputTranslationProfile extends AbstractTranslationProfile<InputTranslationRule>
{
	public enum ProfileMode 
	{
		/**
		 * Actions from the profile are applied and all the data (attributes, ...) 
		 * which is marked as coming from this remote source which is not present after mapping is removed
		 * from the local database.
		 */
		UPDATE_AND_REMOVE_MISSING, 
		
		/**
		 * Actions from the profile are applied only.
		 */
		UPDATE_ONLY
	}
	
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
	private ProfileMode mode;
	
	public InputTranslationProfile(String name, List<InputTranslationRule> rules, ProfileMode mode)
	{
		super(name, ProfileType.INPUT, rules);
		this.mode = mode;
	}

	public InputTranslationProfile(String json, ObjectMapper jsonMapper, TranslationActionsRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}
	
	public ProfileMode getMode()
	{
		return mode;
	}

	public void setMode(ProfileMode mode)
	{
		this.mode = mode;
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

		for (String contextKey : mvelCtx.keySet())
		{
			if (ContextKey.valueOf(contextKey) == null)
			{
				throw new IllegalArgumentException("Incorrect MVEL context, uknnown context key: " + contextKey);
			}
			
			if (mvelCtx.get(contextKey) instanceof Map)
			{
				@SuppressWarnings("unchecked")
				HashMap<String, Object> value = (HashMap<String, Object>) mvelCtx.get(contextKey);
				for (String key : value.keySet())
				{
					exprValMap.put(String.format("%s['%s']", contextKey, key), value.get(key).toString());
				}
			} else if (mvelCtx.get(contextKey) instanceof List)
			{
				exprValMap.put(contextKey, mvelCtx.get(contextKey).toString());
				
			} else if (mvelCtx.get(contextKey) instanceof String)
			{
				exprValMap.put(contextKey, mvelCtx.get(contextKey).toString());
				
			} else
			{
				throw new IllegalArgumentException("Incorrect MVEL context: unexpected: \"" 
						+ mvelCtx.get(contextKey).getClass() 
						+ "\" type for context key: \"" 
						+ contextKey 
						+ "\"");
			}
		}
		
		return exprValMap;
	}
	
	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			storePreable(root);
			root.put("mode", getMode().toString());
			storeRules(root);
			return jsonMapper.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize translation profile to JSON", e);
		}
	}
	
	private void fromJson(String json, ObjectMapper jsonMapper, TranslationActionsRegistry registry)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(json);
			
			loadPreamble(root);
			
			if (root.has("mode"))
			{
				String m = root.get("mode").asText();
				setMode(ProfileMode.valueOf(m));
			} else
				setMode(ProfileMode.UPDATE_ONLY);
			ArrayNode rulesA = (ArrayNode) root.get("rules");
			rules = new ArrayList<>(rulesA.size());
			for (int i=0; i<rulesA.size(); i++)
			{
				ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
				String condition = jsonRule.get("condition").get("conditionValue").asText();
				ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
				String actionName = jsonAction.get("name").asText();
				TranslationActionFactory fact = registry.getByName(actionName);
				String[] parameters = extractParams(jsonAction);
				TranslationAction action = fact.getInstance(parameters);
				if (!(action instanceof InputTranslationAction))
				{
					throw new InternalException("The translation action of the input translation "
							+ "profile is not compatible with it, it is " + action.getClass());
				}
				
				rules.add(new InputTranslationRule((InputTranslationAction) action, 
						new TranslationCondition(condition)));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize translation profile from JSON", e);
		}
	}
}
