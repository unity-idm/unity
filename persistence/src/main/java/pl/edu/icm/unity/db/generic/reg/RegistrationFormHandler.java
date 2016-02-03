/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.RegistrationTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationProfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Handler for {@link RegistrationForm}
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormHandler extends DefaultEntityHandler<RegistrationForm>
{
	public static final String REGISTRATION_FORM_OBJECT_TYPE = "registrationForm";
	private RegistrationTranslationActionsRegistry translationActionsRegistry;
	
	@Autowired
	public RegistrationFormHandler(ObjectMapper jsonMapper, 
			RegistrationTranslationActionsRegistry translationActionsRegistry)
	{
		super(jsonMapper, REGISTRATION_FORM_OBJECT_TYPE, RegistrationForm.class);
		this.translationActionsRegistry = translationActionsRegistry;
	}

	@Override
	public GenericObjectBean toBlob(RegistrationForm value, SqlSession sql)
	{
		try
		{
			ObjectNode root = value.toJson();
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration form to JSON", e);
		}
	}

	@Override
	public RegistrationForm fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			return new RegistrationForm(root);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}
	
	@Override
	public byte[] updateBeforeImport(String name, JsonNode node) throws JsonProcessingException
	{
		if (node == null)
			return null;
		if (node.has("InitialEntityState"))
			updateFromPreTranslationProfileForm((ObjectNode) node);
		return jsonMapper.writeValueAsBytes(node);
	}

	
	/**
	 * Set of changes is as follows:
	 * InitialEntityState -> a rule in translation profile if other then 'valid'
	 * AutoAcceptCondition -> a rule in translation profile if other then 'false'
	 * RedirectAfterSubmit -> a rule in translation profile if non empty
	 * CredentialRequirementAssignment -> changed to default credential assignment
	 * AttributeAssignments, AttributeClassAssignments, GroupAssignments -> all converted to rules in a translation
	 * profile.
	 * @param node
	 */
	private void updateFromPreTranslationProfileForm(ObjectNode node)
	{
		RegistrationTranslationProfileBuilder pBuilder = new RegistrationTranslationProfileBuilder(
				translationActionsRegistry, "formProfile");
		
		if (node.has("InitialEntityState"))
		{
			String initialState = node.get("InitialEntityState").asText();
			if (!EntityState.valid.toString().equals(initialState))
				pBuilder.withInitialState("true", EntityState.valueOf(initialState));
		}
		
		if (node.has("AutoAcceptCondition"))
		{
			String autoAccept = node.get("AutoAcceptCondition").asText("false");
			if (!autoAccept.equals("false"))
				pBuilder.withAutoProcess(autoAccept, AutomaticRequestAction.accept);
		}
		
		if (node.has("RedirectAfterSubmit"))
		{
			String redirect = node.get("RedirectAfterSubmit").asText("");
			if (!redirect.isEmpty())
				pBuilder.withRedirect("true", "'" + redirect + "'");
		}
		
		if (node.has("CredentialRequirementAssignment"))
		{
			String credReq = node.get("CredentialRequirementAssignment").asText();
			node.put("DefaultCredentialRequirement", credReq);
		}
		
		ArrayNode attrAssignements = (ArrayNode) node.get("AttributeAssignments");
		if (attrAssignements != null)
		{
			for (JsonNode aa: attrAssignements)
			{
				ArrayNode values = (ArrayNode) aa.get("values");
				StringJoiner joiner = new StringJoiner("', '", "['", "']");
				values.forEach(v -> {
					String decoded;
					try
					{
						decoded = new String(v.binaryValue(), StandardCharsets.UTF_8);
					} catch (Exception e)
					{
						throw new IllegalStateException("Can't decode attr value", e);
					}
					joiner.add(decoded);
				});
				pBuilder.withAddAttribute("true", 
						aa.get("name").asText(), 
						aa.get("groupPath").asText(), 
						joiner.toString(), 
						AttributeVisibility.valueOf(aa.get("visibility").asText()));
			}
		}

		ArrayNode attrClassAssignements = (ArrayNode) node.get("AttributeClassAssignments");
		if (attrClassAssignements != null)
		{
			for (JsonNode ac: attrClassAssignements)
				pBuilder.withAttributeClass("true", ac.get("group").asText(), 
						"'" + ac.get("acName").asText() + "'");
		}

		ArrayNode groupAssignments = (ArrayNode) node.get("GroupAssignments");
		if (groupAssignments != null)
		{
			for (JsonNode group: groupAssignments)
				pBuilder.withGroupMembership("true", group.asText());
		}

		TranslationProfile profile = pBuilder.build();
		if (!profile.getRules().isEmpty())
			node.set("TranslationProfile", profile.toJsonObject());
		
		node.remove(Lists.newArrayList("InitialEntityState",
				"AutoAcceptCondition",
				"RedirectAfterSubmit",
				"CredentialRequirementAssignment",
				"AttributeAssignments",
				"AttributeClassAssignments",
				"GroupAssignments"));
	}
}



