/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import java.util.List;

import pl.edu.icm.unity.types.DescribedObjectImpl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common code for concrete translation profiles. Includes methods to implements JSON (de)serialization 
 * in the first place. 
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationProfile<T extends AbstractTranslationRule<?>> extends DescribedObjectImpl 
	implements TranslationProfile
{
	protected List<T> rules;
	private ProfileType profileType;

	public AbstractTranslationProfile(String name, ProfileType profileType, List<T> rules)
	{
		this.rules = rules;
		this.profileType = profileType;
		setName(name);
	}

	public AbstractTranslationProfile()
	{
	}

	@Override
	public List<T> getRules()
	{
		return rules;
	}

	public void setRules(List<T> rules)
	{
		this.rules = rules;
	}

	public ProfileType getProfileType()
	{
		return profileType;
	}

	/**
	 * Common data for all profiles
	 * @param root
	 */
	protected void storePreable(ObjectNode root)
	{
		root.put("ver", "2");
		root.put("name", getName());
		root.put("description", getDescription());
		root.put("type", getProfileType().toString());		
	}

	protected void loadPreamble(ObjectNode root)
	{
		setName(root.get("name").asText());
		setDescription(root.get("description").asText());
		if (root.has("type"))
		{
			profileType = ProfileType.valueOf(root.get("type").asText());
		} else
			profileType = ProfileType.INPUT;
	}
	
	/**
	 * Stores array of rules in JSON
	 * @param root
	 */
	protected void storeRules(ObjectNode root)
	{
		ArrayNode jsonRules = root.putArray("rules");
		for (AbstractTranslationRule<?> rule: rules)
		{
			addAction(jsonRules, rule.getAction().getActionDescription().getName(),
					rule.getCondition().getCondition(), 
					rule.getAction().getParameters());
		}
	}
	
	/**
	 * Add a profile's action to JSON array.
	 * @param jsonRules
	 * @param name
	 * @param condition
	 * @param args
	 */
	protected void addAction(ArrayNode jsonRules, String name, String condition, String... args)
	{
		ObjectNode jsonRule = jsonRules.addObject();
		ObjectNode jsonCondition = jsonRule.putObject("condition");
		jsonCondition.put("conditionValue", condition);
		ObjectNode jsonAction = jsonRule.putObject("action");
		jsonAction.put("name", name);
		ArrayNode jsonAParams = jsonAction.putArray("parameters");
		for (String param: args)
			jsonAParams.add(param);
	}
	
	/**
	 * 
	 * @param jsonAction
	 * @return array of action parameter values
	 */
	protected String[] extractParams(ObjectNode jsonAction)
	{
		ArrayNode jsonAParams = (ArrayNode) jsonAction.get("parameters");
		String[] parameters = new String[jsonAParams.size()];
		for (int j=0; j<jsonAParams.size(); j++)
			parameters[j] = jsonAParams.get(j).asText();
		return parameters;
	}
}
