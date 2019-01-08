/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.translation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Definition of a translation profile.
 * @author Krzysztof Benedyczak
 */
public class TranslationProfile extends DescribedObjectImpl
{
	private ProfileType profileType;
	private List<TranslationRule> rules;
	private ProfileMode profileMode;

	public TranslationProfile(String name, String description, ProfileType profileType,
			ProfileMode profileMode, List<? extends TranslationRule> rules)
	{
		super(name, description);
		this.profileType = profileType;
		this.rules = new ArrayList<>(rules);
		this.profileMode = profileMode;
	}
	
	public TranslationProfile(String name, String description, ProfileType profileType,
			 List<? extends TranslationRule> rules)
	{
		this(name, description, profileType, ProfileMode.DEFAULT, rules);
	}
	
	@JsonCreator
	public TranslationProfile(ObjectNode json)
	{
		fromJson(json);
	}
	
	public ProfileType getProfileType()
	{
		return profileType;
	}
	
	public ProfileMode getProfileMode()
	{
		return profileMode;
	}
	
	public void setProfileMode(ProfileMode profileMode)
	{
		this.profileMode = profileMode;
		
	}
	
	public List<? extends TranslationRule> getRules()
	{
		return rules;
	}

	@JsonValue
	public ObjectNode toJsonObject()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		storePreable(root);
		storeRules(root);
		return root;
	}
	
	private void storePreable(ObjectNode root)
	{
		root.put("ver", "2");
		root.put("name", getName());
		if (getDescription() != null)
			root.put("description", getDescription());
		root.put("type", getProfileType().toString());	
		root.put("mode", getProfileMode().toString());		
	}

	private void storeRules(ObjectNode root)
	{
		ArrayNode jsonRules = root.putArray("rules");
		for (TranslationRule rule: rules)
			storeRule(jsonRules, rule);
	}

	public static void storeRule(ArrayNode jsonRules, TranslationRule rule)
	{
		ObjectNode jsonRule = jsonRules.addObject();
		ObjectNode jsonCondition = jsonRule.putObject("condition");
		jsonCondition.put("conditionValue", rule.getCondition());
		ObjectNode jsonAction = jsonRule.putObject("action");
		TranslationAction action = rule.getAction();
		jsonAction.put("name", action.getName());
		ArrayNode jsonAParams = jsonAction.putArray("parameters");
		for (String param: action.getParameters())
			jsonAParams.add(param);
	}
	
	
	private void fromJson(ObjectNode root)
	{
		try
		{
			loadPreamble(root);

			ArrayNode rulesA = (ArrayNode) root.get("rules");
			rules = new ArrayList<>(rulesA.size());
			for (int i=0; i<rulesA.size(); i++)
			{
				ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
				rules.add(loadRule(jsonRule));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize translation profile from JSON", e);
		}
	}

	private TranslationRule loadRule(ObjectNode jsonRule)
	{
		String condition = jsonRule.get("condition").get("conditionValue").asText();
		ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
		String actionName = jsonAction.get("name").asText();
		String[] parameters = extractParams(jsonAction);
		TranslationAction action = new TranslationAction(actionName, parameters);
		return new TranslationRule(condition, action);
	}
	
	private void loadPreamble(ObjectNode root)
	{
		this.name = root.get("name").asText();
		if (root.has("description") && !root.get("description").isNull())
			this.description = root.get("description").asText();
		if (root.has("type"))
			profileType = ProfileType.valueOf(root.get("type").asText());
		else
			profileType = ProfileType.INPUT;
		if (root.has("mode"))
		{
			try
			{
				profileMode = ProfileMode.valueOf(root.get("mode").asText());
			} catch (Exception e)
			{
				profileMode = ProfileMode.DEFAULT;
			}
		} else
		{
			profileMode = ProfileMode.DEFAULT;
		}
	}

	private String[] extractParams(ObjectNode jsonAction)
	{
		ArrayNode jsonAParams = (ArrayNode) jsonAction.get("parameters");
		String[] parameters = new String[jsonAParams.size()];
		for (int j=0; j<jsonAParams.size(); j++)
			parameters[j] = jsonAParams.get(j).isNull() ? null : jsonAParams.get(j).asText();
		return parameters;
	}
	
	@Override
	public String toString()
	{
		return "TranslationProfile [profileType=" + profileType + ", name=" + name
				+ ", profileMode=" + profileMode + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((profileType == null) ? 0 : profileType.hashCode());
		result = prime * result + ((rules == null) ? 0 : rules.hashCode());
		result = prime * result + ((profileMode == null) ? 0 : profileMode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TranslationProfile other = (TranslationProfile) obj;
		if (profileType != other.profileType)
			return false;
		if (rules == null)
		{
			if (other.rules != null)
				return false;
		} else if (!rules.equals(other.rules))
			return false;
		if (profileMode != other.profileMode)
			return false;
		
		return super.equals(obj);
	}
	
	public TranslationProfile clone()
	{
		ObjectNode p = toJsonObject();
		return new TranslationProfile(p);	
	}
}
