/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Base of enquiry and registration requests.
 * 
 * @author K. Benedyczak
 */
public class BaseRegistrationInput
{
	private String formId;
	private List<IdentityParam> identities = new ArrayList<>();
	private List<Attribute> attributes = new ArrayList<>();
	private List<CredentialParamValue> credentials = new ArrayList<>();
	private List<GroupSelection> groupSelections = new ArrayList<>();
	private List<Selection> agreements = new ArrayList<>();
	private String comments;
	private String userLocale;
	private String registrationCode;
	
	public BaseRegistrationInput()
	{
	}

	@JsonCreator
	public BaseRegistrationInput(ObjectNode root)
	{
		try
		{
			fromJson(root);
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Provided JSON is invalid", e);
		}
	}
	
	public void validate()
	{
		if (formId == null)
			throw new IllegalStateException("Form id must be set");
	}
	
	public String getFormId()
	{
		return formId;
	}

	public void setFormId(String formId)
	{
		this.formId = formId;
	}
	public List<IdentityParam> getIdentities()
	{
		return identities;
	}
	public void setIdentities(List<IdentityParam> identities)
	{
		this.identities = identities;
	}

	public List<Attribute> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes)
	{
		this.attributes = attributes;
	}

	public List<CredentialParamValue> getCredentials()
	{
		return credentials;
	}

	public void setCredentials(List<CredentialParamValue> credentials)
	{
		this.credentials = credentials;
	}

	public List<GroupSelection> getGroupSelections()
	{
		return groupSelections;
	}

	public void setGroupSelections(List<GroupSelection> groupSelections)
	{
		this.groupSelections = groupSelections;
	}

	public void addGroupSelection(GroupSelection groupSelection)
	{
		this.groupSelections.add(groupSelection);
	}

	public List<Selection> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<Selection> agreements)
	{
		this.agreements = agreements;
	}

	public String getComments()
	{
		return comments;
	}

	public void setComments(String comments)
	{
		this.comments = comments;
	}

	public String getUserLocale()
	{
		return userLocale;
	}

	public void setUserLocale(String userLocale)
	{
		this.userLocale = userLocale;
	}
	
	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	@Override
	public String toString()
	{
		return "BaseRegistrationInput [formId=" + formId + ", identities=" + identities
				+ "]";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("Agreements", jsonMapper.valueToTree(getAgreements()));
		root.set("Attributes", jsonMapper.valueToTree(getAttributes()));
		root.set("Comments", jsonMapper.valueToTree(getComments()));
		root.set("Credentials", jsonMapper.valueToTree(getCredentials()));
		root.set("FormId", jsonMapper.valueToTree(getFormId()));
		root.set("GroupSelections", jsonMapper.valueToTree(getGroupSelections()));
		root.set("Identities", jsonMapper.valueToTree(getIdentities()));
		root.put("UserLocale", getUserLocale());
		root.put("RegistrationCode", registrationCode);
		return root;
	}

	private void fromJson(ObjectNode root) throws IOException
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		JsonNode n = root.get("Agreements");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<Selection> r = jsonMapper.readValue(v, 
					new TypeReference<List<Selection>>(){});
			setAgreements(r);
		}
		
		n = root.get("Attributes");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<Attribute> r = jsonMapper.readValue(v, 
					new TypeReference<List<Attribute>>(){});
			setAttributes(r);	
		}
	
		n = root.get("Comments");
		if (n != null && !n.isNull())
			setComments(n.asText());
		
		n = root.get("Credentials");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<CredentialParamValue> r = jsonMapper.readValue(v, 
					new TypeReference<List<CredentialParamValue>>(){});
			setCredentials(r);
		}
		
		n = root.get("FormId");
		setFormId(n.asText());

		n = root.get("GroupSelections");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<GroupSelection> r = jsonMapper.readValue(v, 
					new TypeReference<List<GroupSelection>>(){});
			setGroupSelections(r);
		}

		n = root.get("Identities");			
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<IdentityParam> r = jsonMapper.readValue(v, 
					new TypeReference<List<IdentityParam>>(){});
			setIdentities(r);
		}
		
		n = root.get("UserLocale");
		if (n != null && !n.isNull())
			setUserLocale(n.asText());
		
		n = root.get("RegistrationCode");
		if (n != null && !n.isNull())
			setRegistrationCode(n.asText());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(agreements, attributes, comments, credentials, formId, groupSelections, identities,
				userLocale, registrationCode);
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
		BaseRegistrationInput other = (BaseRegistrationInput) obj;
		return Objects.equals(this.agreements, other.agreements)
				&& Objects.equals(this.attributes, other.attributes)
				&& Objects.equals(this.comments, other.comments)
				&& Objects.equals(this.credentials, other.credentials)
				&& Objects.equals(this.formId, other.formId)
				&& Objects.equals(this.groupSelections, other.groupSelections)
				&& Objects.equals(this.identities, other.identities)
				&& Objects.equals(this.userLocale, other.userLocale)
				&& Objects.equals(this.registrationCode, other.registrationCode);

	}
}
