/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.NDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Full profile of translation: a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class TranslationProfile extends DescribedObjectImpl
{
	private List<TranslationRule> rules;

	public TranslationProfile(String name, List<TranslationRule> rules)
	{
		this.rules = rules;
		setName(name);
	}

	public TranslationProfile(String json, ObjectMapper jsonMapper, TranslationActionsRegistry registry)
	{
		fromJson(json, jsonMapper, registry);
	}

	public List<TranslationRule> getRules()
	{
		return rules;
	}

	public void setRules(List<TranslationRule> rules)
	{
		this.rules = rules;
	}
	
	public void translate(RemotelyAuthenticatedInput input) throws EngineException
	{
		NDC.push("[TrProfile " + getName() + "]");
		try
		{
			int i=1;
			for (TranslationRule rule: rules)
			{
				NDC.push("[r: " + (i++) + "]");
				try
				{
					rule.invoke(input);
				} catch (ExecutionBreakException e)
				{
					break;
				} finally
				{
					NDC.pop();
				}
			}
		} finally
		{
			NDC.pop();
		}
	}
	
	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("name", getName());
			root.put("description", getDescription());
			ArrayNode jsonRules = root.putArray("rules");
			for (TranslationRule rule: rules)
			{
				ObjectNode jsonRule = jsonRules.addObject();
				ObjectNode jsonCondition = jsonRule.putObject("condition");
				jsonCondition.put("conditionValue", rule.getCondition().getCondition());
				ObjectNode jsonAction = jsonRule.putObject("action");
				jsonAction.put("name", rule.getAction().getName());
				ArrayNode jsonAParams = jsonAction.putArray("parameters");
				for (String param: rule.getAction().getParameters())
					jsonAParams.add(param);
			}
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
			setName(root.get("name").asText());
			setDescription(root.get("description").asText());
			ArrayNode rulesA = (ArrayNode) root.get("rules");
			rules = new ArrayList<>(rulesA.size());
			for (int i=0; i<rulesA.size(); i++)
			{
				ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
				String condition = jsonRule.get("condition").get("conditionValue").asText();
				ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
				String actionName = jsonAction.get("name").asText();
				TranslationActionFactory fact = registry.getByName(actionName);
				ArrayNode jsonAParams = (ArrayNode) jsonAction.get("parameters");
				String[] parameters = new String[jsonAParams.size()];
				for (int j=0; j<jsonAParams.size(); j++)
					parameters[j] = jsonAParams.get(j).asText();
				TranslationAction action = fact.getInstance(parameters);
				
				rules.add(new TranslationRule(action, new TranslationCondition(condition)));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize translation profile from JSON", e);
		}
	}
}
