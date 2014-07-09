/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entry point: output translation profile, a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class OutputTranslationProfile extends AbstractTranslationProfile<OutputTranslationRule>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, OutputTranslationProfile.class);
	
	public OutputTranslationProfile(String name, List<OutputTranslationRule> rules)
	{
		super(name, ProfileType.OUTPUT, rules);
	}

	public OutputTranslationProfile(String json, ObjectMapper jsonMapper, TranslationActionsRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}
	
	public TranslationResult translate(TranslationInput input) throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		if (log.isDebugEnabled())
			log.debug("Unprocessed data from local database:\n" + input.getTextDump());
		Object mvelCtx = createMvelContext(input);
		try
		{
			int i=1;
			TranslationResult translationState = new TranslationResult();
			for (OutputTranslationRule rule: rules)
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
	
	public static Map<String, Object> createMvelContext(TranslationInput input)
	{
		Map<String, Object> ret = new HashMap<>();
		
		ret.put("protocol", input.getProtocol());
		ret.put("protocolSubtype", input.getProtocolSubType());
		ret.put("requester", input.getRequester());
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, List<? extends Object>> attrs = new HashMap<String, List<?>>();
		for (Attribute<?> ra: input.getAttributes())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put("attr", attr);
		ret.put("attrs", attrs);
		
		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (Identity id: input.getEntity().getIdentities())
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
		return ret;
	}
	
	
	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			storePreable(root);
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
				if (!(action instanceof OutputTranslationAction))
				{
					throw new InternalException("The translation action of the input translation "
							+ "profile is not compatible with it, it is " + action.getClass());
				}
				
				rules.add(new OutputTranslationRule((OutputTranslationAction) action, 
						new TranslationCondition(condition)));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize translation profile from JSON", e);
		}
	}
}
