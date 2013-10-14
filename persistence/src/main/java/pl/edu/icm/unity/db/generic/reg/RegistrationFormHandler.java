/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

/**
 * Handler for {@link CredentialDefinition}
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormHandler extends DefaultEntityHandler<RegistrationForm>
{
	public static final String REGISTRATION_FORM_OBJECT_TYPE = "registrationForm";
	private FullAttributeSerializer attributeSerializer;
	
	@Autowired
	public RegistrationFormHandler(ObjectMapper jsonMapper, FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, REGISTRATION_FORM_OBJECT_TYPE, RegistrationForm.class);
		this.attributeSerializer = attributeSerializer;
	}

	@Override
	public GenericObjectBean toBlob(RegistrationForm value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("Agreements", jsonMapper.valueToTree(value.getAgreements()));
			addAttributes(root, value.getAttributeAssignments());
			root.put("AttributeClassAssignments", jsonMapper.valueToTree(value.getAttributeClassAssignments()));
			root.put("AttributeParams", jsonMapper.valueToTree(value.getAttributeParams()));
			root.put("CollectComments", value.isCollectComments());
			root.put("CredentialParams", jsonMapper.valueToTree(value.getCredentialParams()));
			root.put("CredentialRequirementAssignment", value.getCredentialRequirementAssignment());
			root.put("Description", value.getDescription());
			root.put("FormInformation", value.getFormInformation());
			root.put("GroupAssignments", jsonMapper.valueToTree(value.getGroupAssignments()));
			root.put("GroupParams", jsonMapper.valueToTree(value.getGroupParams()));
			root.put("IdentityParams", jsonMapper.valueToTree(value.getIdentityParams()));
			root.put("InitialEntityState", jsonMapper.valueToTree(value.getInitialEntityState().name()));
			root.put("Name", value.getName());
			root.put("NotificationsConfiguration", jsonMapper.valueToTree(value.getNotificationsConfiguration()));
			root.put("PubliclyAvailable", value.isPubliclyAvailable());
			root.put("RegistrationCode", value.getRegistrationCode());
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration form to JSON", e);
		}
	}

	private void addAttributes(ObjectNode root, List<Attribute<?>> attributes)
	{
		ArrayNode jsonAttrs = root.putArray("AttributeAssignments");
		for (Attribute<?> a: attributes)
		{
			jsonAttrs.add(attributeSerializer.toJson(a));
		}
	}
	
	private List<Attribute<?>> getAttributes(ArrayNode n, SqlSession sql) 
			throws IllegalAttributeTypeException, IllegalTypeException
	{
		List<Attribute<?>> ret = new ArrayList<>(n.size());
		for (int i=0; i<n.size(); i++)
		{
			Attribute<?> a = attributeSerializer.fromJson((ObjectNode) n.get(i), sql);
			ret.add(a);
		}
		return ret;
	}
	
	@Override
	public RegistrationForm fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			RegistrationForm ret = new RegistrationForm();
			JsonNode n = root.get("Agreements");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AgreementRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<AgreementRegistrationParam>>(){});
				ret.setAgreements(r);
			}
			
			n = root.get("AttributeAssignments");
			ret.setAttributeAssignments(getAttributes((ArrayNode) n, sql));
			
			
			n = root.get("AttributeClassAssignments");
			if (n!= null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AttributeClassAssignment> r = jsonMapper.readValue(v, 
						new TypeReference<List<AttributeClassAssignment>>(){});
				ret.setAttributeClassAssignments(r);
			}
			n = root.get("AttributeParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AttributeRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<AttributeRegistrationParam>>(){});
				ret.setAttributeParams(r);
			}
			n = root.get("CollectComments");
			ret.setCollectComments(n.asBoolean());
			n = root.get("CredentialParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<CredentialRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<CredentialRegistrationParam>>(){});
				ret.setCredentialParams(r);
			}
			n = root.get("CredentialRequirementAssignment");
			ret.setCredentialRequirementAssignment(n == null ? null : n.asText());
			n = root.get("Description");
			ret.setDescription(n == null ? null : n.asText());
			n = root.get("FormInformation");
			ret.setFormInformation(n == null ? null : n.asText());
			n = root.get("GroupAssignments");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<String> r = jsonMapper.readValue(v, 
						new TypeReference<List<String>>(){});
				ret.setGroupAssignments(r);
			}

			n = root.get("GroupParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<GroupRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<GroupRegistrationParam>>(){});
				ret.setGroupParams(r);
			}

			n = root.get("IdentityParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<IdentityRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<IdentityRegistrationParam>>(){});
				ret.setIdentityParams(r);
			}
			n = root.get("InitialEntityState");
			ret.setInitialEntityState(n == null ? EntityState.valid : EntityState.valueOf(n.asText()));
			
			n = root.get("Name");
			ret.setName(n.asText());
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				RegistrationFormNotifications r = jsonMapper.readValue(v, 
						new TypeReference<RegistrationFormNotifications>(){});
				ret.setNotificationsConfiguration(r);
			}

			n = root.get("PubliclyAvailable");
			ret.setPubliclyAvailable(n.asBoolean());
			n = root.get("RegistrationCode");
			ret.setRegistrationCode(n == null ? null : n.asText());
			
			return ret;
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}
}
