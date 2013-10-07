/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Registration request, tied to a registration form contains data collected during registration process.
 * This data can be entered by the user in UI, taken from external IdP or possibly from other 
 * sources (e.g. a DN can be taken from client-authenticated TLS).
 * 
 * @author K. Benedyczak
 */
public class RegistrationRequest
{
	private Date timestamp;
	private String formId;
	
	private List<IdentityParam> identities;
	private List<AttributeParamValue> attributes;
	private List<CredentialParamValue> credentials;
	private List<Selection> groupSelections;
	private List<Selection> agreements;
	private String comments;
	private String registrationCode;
	
	
	public Date getTimestamp()
	{
		return timestamp;
	}
	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
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
	public List<AttributeParamValue> getAttributes()
	{
		return attributes;
	}
	public void setAttributes(List<AttributeParamValue> attributes)
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
	public List<Selection> getGroupSelections()
	{
		return groupSelections;
	}
	public void setGroupSelections(List<Selection> groupSelections)
	{
		this.groupSelections = groupSelections;
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
	public String getRegistrationCode()
	{
		return registrationCode;
	}
	public void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}
}
