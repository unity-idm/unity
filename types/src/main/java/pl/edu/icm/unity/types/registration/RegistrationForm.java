/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;

import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Configuration of a registration form. Registration form data contains:
 * <ol>
 *  <li> its identification and description,
 *  <li> configuration of what information is collected during registration (and in how),
 *  <li> extra information to be presented to the user
 *  <li> automatic assignments, which are used to for the user whose registration is accepted. This is 
 *  useful for automation of users adding by administrator.
 *  <li> visibility, which controls whether the form is publicly available for all (anonymous) clients or
 *  whether only for authorized administrators. 
 * </ol>
 * @author K. Benedyczak
 */
public class RegistrationForm extends DescribedObjectImpl
{
	private boolean publiclyAvailable;
	
	private List<IdentityRegistrationParam> identityParams;
	private List<AttributeRegistrationParam> attributeParams;	
	private List<GroupRegistrationParam> groupParams;
	private List<CredentialRegistrationParam> credentialParams;
	private List<AgreementRegistrationParam> agreements;
	private boolean collectComments;
	private String formInformation;
	private String registrationCode;
	
	private String credentialRequirementAssignment;
	private List<Attribute<?>> attributeAssignments;
	private List<String> groupAssignments;
	private List<AttributeClassAssignment> attributeClassAssignments;
	
	
	public List<IdentityRegistrationParam> getIdentityParams()
	{
		return identityParams;
	}

	public void setIdentityParams(List<IdentityRegistrationParam> identityParams)
	{
		this.identityParams = identityParams;
	}

	public List<AttributeRegistrationParam> getAttributeParams()
	{
		return attributeParams;
	}

	public void setAttributeParams(List<AttributeRegistrationParam> attributeParams)
	{
		this.attributeParams = attributeParams;
	}

	public List<GroupRegistrationParam> getGroupParams()
	{
		return groupParams;
	}

	public void setGroupParams(List<GroupRegistrationParam> groupParams)
	{
		this.groupParams = groupParams;
	}

	public List<CredentialRegistrationParam> getCredentialParams()
	{
		return credentialParams;
	}

	public void setCredentialParams(List<CredentialRegistrationParam> credentialParams)
	{
		this.credentialParams = credentialParams;
	}

	public List<AgreementRegistrationParam> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<AgreementRegistrationParam> agreements)
	{
		this.agreements = agreements;
	}

	public boolean isCollectComments()
	{
		return collectComments;
	}

	public void setCollectComments(boolean collectComments)
	{
		this.collectComments = collectComments;
	}

	public String getFormInformation()
	{
		return formInformation;
	}

	public void setFormInformation(String formInformation)
	{
		this.formInformation = formInformation;
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	public void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}

	public String getCredentialRequirementAssignment()
	{
		return credentialRequirementAssignment;
	}

	public void setCredentialRequirementAssignment(String credentialRequirementAssignment)
	{
		this.credentialRequirementAssignment = credentialRequirementAssignment;
	}

	public List<Attribute<?>> getAttributeAssignments()
	{
		return attributeAssignments;
	}

	public void setAttributeAssignments(List<Attribute<?>> attributeAssignments)
	{
		this.attributeAssignments = attributeAssignments;
	}

	public List<String> getGroupAssignments()
	{
		return groupAssignments;
	}

	public void setGroupAssignments(List<String> groupAssignments)
	{
		this.groupAssignments = groupAssignments;
	}

	public List<AttributeClassAssignment> getAttributeClassAssignments()
	{
		return attributeClassAssignments;
	}

	public void setAttributeClassAssignments(List<AttributeClassAssignment> attributeClassAssignments)
	{
		this.attributeClassAssignments = attributeClassAssignments;
	}
}
