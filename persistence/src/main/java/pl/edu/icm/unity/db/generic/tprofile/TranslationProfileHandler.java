/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.tprofile;

import java.nio.charset.StandardCharsets;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Handler for {@link AbstractTranslationProfile}.
 * 
 * @author K. Benedyczak
 */
@Component
public class TranslationProfileHandler extends DefaultEntityHandler<TranslationProfile>
{
	public static final String TRANSLATION_PROFILE_OBJECT_TYPE = "translationProfile";
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, TranslationProfileHandler.class);
	private TranslationActionsRegistry actionsRegistry;
	
	@Autowired
	public TranslationProfileHandler(ObjectMapper jsonMapper, TranslationActionsRegistry actionsRegistry)
	{
		super(jsonMapper, TRANSLATION_PROFILE_OBJECT_TYPE, TranslationProfile.class);
		this.actionsRegistry = actionsRegistry;
	}

	@Override
	public GenericObjectBean toBlob(TranslationProfile value, SqlSession sql)
	{
		String json = value.toJson(jsonMapper);
		return new GenericObjectBean(value.getName(), json.getBytes(StandardCharsets.UTF_8), supportedType, 
				value.getProfileType().toString());
	}

	@Override
	public TranslationProfile fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		String subType = blob.getSubType();
		if (subType == null)
			subType = ProfileType.INPUT.toString();
		ProfileType pt = ProfileType.valueOf(subType);
		switch (pt)
		{
		case INPUT:
			return new InputTranslationProfile(new String(blob.getContents(), StandardCharsets.UTF_8), 
					jsonMapper, actionsRegistry);
		case OUTPUT:
			return new OutputTranslationProfile(new String(blob.getContents(), StandardCharsets.UTF_8), 
					jsonMapper, actionsRegistry);
		}
		throw new IllegalStateException("The stored translation profile with subtype id " + subType + 
				" has no implemented class representation");
	}
	
	@Override
	public byte[] updateBeforeImport(String name, JsonNode node) throws JsonProcessingException
	{
		if (node == null)
			return null;
		
		ObjectNode tp = (ObjectNode) node;
		if (!tp.has("ver"))
			node = convertV1(tp, jsonMapper);
		
		return jsonMapper.writeValueAsBytes(node);
	}
	
	
	
	/**
	 * Legacy syntax converter.
	 * @param old
	 * @param json
	 * @param jsonMapper
	 * @return
	 * @throws JsonProcessingException 
	 */
	private ObjectNode convertV1(ObjectNode old, ObjectMapper jsonMapper) throws JsonProcessingException
	{
		String name = old.get("name").asText();
		String json = jsonMapper.writeValueAsString(old);
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
		root.set("description", old.get("description"));
		root.put("mode", InputTranslationProfile.ProfileMode.UPDATE_ONLY.toString());
		ArrayNode jsonRules = root.putArray("rules");

		String idMode = (createUserCond != null) ? IdentityEffectMode.CREATE_OR_MATCH.toString() 
				: IdentityEffectMode.MATCH.toString();

		for (int i=0; i<rulesA.size(); i++)
		{
			ObjectNode jsonRule = (ObjectNode) rulesA.get(i);
			String condition = jsonRule.get("condition").get("conditionValue").asText();
			ObjectNode jsonAction = (ObjectNode) jsonRule.get("action");
			String actionName = jsonAction.get("name").asText();
			String[] oldParams = AbstractTranslationProfile.extractParams(jsonAction);
			
			if (actionName.equals("mapIdentityByType"))
			{
				AbstractTranslationProfile.addAction(jsonRules, "mapIdentity", 
						createUserCond != null ? createUserCond : condition, 
						oldParams[1], "idsByType['" + oldParams[0] + "']", 
						oldParams[2], idMode);
			}
			
			if (actionName.equals("mapGroup") && updateGroupsCond != null)
			{
				String group = oldParams[1].equals("$1") ? oldParams[0] : oldParams[1];
				AbstractTranslationProfile.addAction(jsonRules, "mapGroup", updateGroupsCond, 
						"'" + group + "'");
				log.warn("Please re-check the group mapping: " + group);
			}
			
			if (actionName.equals("mapAttributeToIdentity"))
			{
				AbstractTranslationProfile.addAction(jsonRules, "mapIdentity", 
						createUserCond != null ? createUserCond : condition, 
						oldParams[1], "attr['" + oldParams[0] + "']", 
						oldParams[2], idMode);
			}
			
			if (actionName.equals("mapAttribute") && updateAttributesCond != null)
			{
				String attr = oldParams[1].equals("$1") ? oldParams[0] : oldParams[1];
				AbstractTranslationProfile.addAction(jsonRules, "mapAttribute", updateAttributesCond, 
						attr, oldParams[2], 
						"attr['" + oldParams[0] + "']", 
						AttributeVisibility.full.toString(), "CREATE_OR_UPDATE");
			}
			
			if (actionName.equals("mapIdentity"))
			{
				AbstractTranslationProfile.addAction(jsonRules, "mapIdentity", 
						createUserCond != null ? createUserCond : condition, 
						"identifier", "id", 
						oldParams[2], idMode);
				log.warn("Please re-check the identity mapping, was: " + oldParams[0] + 
						" -> " + oldParams[1] + ". Currently mapped to identifier type "
								+ "with default remote identity.");
			}
		}
		
		return root;
	}

}
