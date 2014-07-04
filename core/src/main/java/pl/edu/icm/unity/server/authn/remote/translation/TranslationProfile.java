/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Full profile of translation: a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class TranslationProfile extends DescribedObjectImpl
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
	
	private Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, TranslationProfile.class);
	private List<TranslationRule> rules;
	private ProfileMode mode;

	public TranslationProfile(String name, List<TranslationRule> rules, ProfileMode mode)
	{
		this.rules = rules;
		this.mode = mode;
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
			for (TranslationRule rule: rules)
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
		
		ret.put("idp", input.getIdpName());
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, List<Object>> attrs = new HashMap<String, List<Object>>();
		for (RemoteAttribute ra: input.getAttributes().values())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put("attr", attr);
		ret.put("attrs", attrs);
		
		if (!input.getIdentities().isEmpty())
		{
			RemoteIdentity ri = input.getIdentities().values().iterator().next();
			ret.put("id", ri.getName());
			ret.put("idType", ri.getIdentityType());
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
		ret.put("idsByType", idsByType);
		
		ret.put("groups", new ArrayList<String>(input.getGroups().keySet()));
		return ret;
	}
	
	public String toJson(ObjectMapper jsonMapper)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("ver", "2");
			root.put("name", getName());
			root.put("description", getDescription());
			root.put("mode", getMode().toString());
			ArrayNode jsonRules = root.putArray("rules");
			for (TranslationRule rule: rules)
			{
				addAction(jsonRules, rule.getAction().getActionDescription().getName(),
						rule.getCondition().getCondition(), 
						rule.getAction().getParameters());
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
			if (!root.has("ver"))
				root = convertV1(root, json, jsonMapper);
			
			setName(root.get("name").asText());
			setDescription(root.get("description").asText());
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
				
				rules.add(new TranslationRule(action, new TranslationCondition(condition)));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize translation profile from JSON", e);
		}
	}
	
	private ObjectNode convertV1(ObjectNode old, String json, ObjectMapper jsonMapper)
	{
		String name = old.get("name").asText();
		log.warn("The translation profile " + name + " is in legacy format. The profile will be recreated. "
				+ "Please VERIFY it manually, especially if there are any warning below. "
				+ "The old profile dump follows. "
				+ "In case of any troubles provide it to the support mailing list, "
				+ "we will help you to create a new profile.\n" + json);
		
		ArrayNode rulesA = (ArrayNode) old.get("rules");
		String createUserCond = null;
		String updateAttributesCond = null;
		String updateGroupsCond = null;
		for (int i=0; i<rulesA.size(); i++)
		{
			ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
			String condition = jsonRule.get("condition").get("conditionValue").asText();
			ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
			String actionName = jsonAction.get("name").asText();
			if (actionName.equals("createUser"))
				createUserCond = condition;
			if (actionName.equals("updateAttributes"))
				updateAttributesCond = condition;
			if (actionName.equals("updateGroups"))
				updateGroupsCond = condition;
		}
		
		
		ObjectNode root = jsonMapper.createObjectNode();
		root.put("ver", "2");
		root.put("name", name);
		root.put("description", old.get("description"));
		root.put("mode", ProfileMode.UPDATE_ONLY.toString());
		ArrayNode jsonRules = root.putArray("rules");

		for (int i=0; i<rulesA.size(); i++)
		{
			ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
			ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
			String actionName = jsonAction.get("name").asText();
			String[] oldParams = extractParams(jsonAction);
			
			if (actionName.equals("mapIdentityByType") && createUserCond != null)
			{
				addAction(jsonRules, "mapIdentity", createUserCond, 
						oldParams[1], "idsByType['" + oldParams[0] + "']", 
						oldParams[2], "CREATE_OR_MATCH");
			}
			
			if (actionName.equals("mapGroup") && updateGroupsCond != null)
			{
				String group = oldParams[1].equals("$1") ? oldParams[0] : oldParams[1];
				addAction(jsonRules, "mapGroup", updateGroupsCond, 
						"'" + group + "'");
				log.warn("Please re-check the group mapping: " + group);
			}
			
			if (actionName.equals("mapAttributeToIdentity") && createUserCond != null)
			{
				addAction(jsonRules, "mapIdentity", createUserCond, 
						oldParams[1], "attr['" + oldParams[0] + "']", 
						oldParams[2], "CREATE_OR_MATCH");
			}
			
			if (actionName.equals("mapAttribute") && updateAttributesCond != null)
			{
				String attr = oldParams[1].equals("$1") ? oldParams[0] : oldParams[1];
				addAction(jsonRules, "mapAttribute", updateAttributesCond, 
						attr, oldParams[2], 
						"attr['" + oldParams[0] + "']", 
						AttributeVisibility.full.toString(), "CREATE_OR_UPDATE");
			}
			
			if (actionName.equals("mapIdentity") && createUserCond != null)
			{
				addAction(jsonRules, "mapIdentity", createUserCond, 
						"identifier", "id", 
						oldParams[2], "CREATE_OR_MATCH");
				log.warn("Please re-check the identity mapping, was: " + oldParams[0] + 
						" -> " + oldParams[1] + ". Currently mapped to identifier type "
								+ "with default remote identity.");
			}
		}
		
		return root;
	}
	
	private void addAction(ArrayNode jsonRules, String name, String condition, String... args)
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
	
	private String[] extractParams(ObjectNode jsonAction)
	{
		ArrayNode jsonAParams = (ArrayNode) jsonAction.get("parameters");
		String[] parameters = new String[jsonAParams.size()];
		for (int j=0; j<jsonAParams.size(); j++)
			parameters[j] = jsonAParams.get(j).asText();
		return parameters;
	}
}
