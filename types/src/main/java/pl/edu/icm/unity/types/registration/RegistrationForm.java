/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;

import pl.edu.icm.unity.types.DescribedObjectImpl;
import pl.edu.icm.unity.types.EntityState;
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
	private RegistrationFormNotifications notificationsConfiguration = new RegistrationFormNotifications();
	
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
	private EntityState initialEntityState;
	private String autoAcceptCondition;
		
	public String getAutoAcceptCondition()
	{
		return autoAcceptCondition;
	}

	public void setAutoAcceptCondition(String autoAcceptCondition)
	{
		this.autoAcceptCondition = autoAcceptCondition;
	}

	public RegistrationFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}

	public void setNotificationsConfiguration(RegistrationFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
	}

	public EntityState getInitialEntityState()
	{
		return initialEntityState;
	}

	public void setInitialEntityState(EntityState initialEntityState)
	{
		this.initialEntityState = initialEntityState;
	}

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
	
	public boolean containsAutomaticAndMandatoryParams()
	{
		for (IdentityRegistrationParam id : identityParams)
		{
			if (checkAutoParam(id))
				return true;
		}

		for (AttributeRegistrationParam at : attributeParams)
		{
			if (checkAutoParam(at))
				return true;
		}
		
		for (GroupRegistrationParam gr:groupParams)
		{
			if (gr.getRetrievalSettings() == ParameterRetrievalSettings.automatic
					|| gr.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
				return true;
		}
		return false;
	}
	
	private boolean checkAutoParam(OptionalRegistrationParam param)
	{
		if (!param.isOptional()
				&& (param.getRetrievalSettings() == ParameterRetrievalSettings.automatic || param
						.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
			return true;

		return false;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((agreements == null) ? 0 : agreements.hashCode());
		result = prime
				* result
				+ ((attributeAssignments == null) ? 0 : attributeAssignments
						.hashCode());
		result = prime
				* result
				+ ((attributeClassAssignments == null) ? 0
						: attributeClassAssignments.hashCode());
		result = prime * result
				+ ((attributeParams == null) ? 0 : attributeParams.hashCode());
		result = prime * result + (collectComments ? 1231 : 1237);
		result = prime * result
				+ ((credentialParams == null) ? 0 : credentialParams.hashCode());
		result = prime
				* result
				+ ((credentialRequirementAssignment == null) ? 0
						: credentialRequirementAssignment.hashCode());
		result = prime * result
				+ ((formInformation == null) ? 0 : formInformation.hashCode());
		result = prime * result
				+ ((groupAssignments == null) ? 0 : groupAssignments.hashCode());
		result = prime * result + ((groupParams == null) ? 0 : groupParams.hashCode());
		result = prime * result
				+ ((identityParams == null) ? 0 : identityParams.hashCode());
		result = prime
				* result
				+ ((initialEntityState == null) ? 0 : initialEntityState.hashCode());
		result = prime
				* result
				+ ((notificationsConfiguration == null) ? 0
						: notificationsConfiguration.hashCode());
		result = prime * result + (publiclyAvailable ? 1231 : 1237);
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
		
		result = prime * result
				+ ((autoAcceptCondition == null) ? 0 : autoAcceptCondition
						.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegistrationForm other = (RegistrationForm) obj;
		if (agreements == null)
		{
			if (other.agreements != null)
				return false;
		} else if (!agreements.equals(other.agreements))
			return false;
		if (attributeAssignments == null)
		{
			if (other.attributeAssignments != null)
				return false;
		} else if (!attributeAssignments.equals(other.attributeAssignments))
			return false;
		if (attributeClassAssignments == null)
		{
			if (other.attributeClassAssignments != null)
				return false;
		} else if (!attributeClassAssignments.equals(other.attributeClassAssignments))
			return false;
		if (attributeParams == null)
		{
			if (other.attributeParams != null)
				return false;
		} else if (!attributeParams.equals(other.attributeParams))
			return false;
		if (collectComments != other.collectComments)
			return false;
		if (credentialParams == null)
		{
			if (other.credentialParams != null)
				return false;
		} else if (!credentialParams.equals(other.credentialParams))
			return false;
		if (credentialRequirementAssignment == null)
		{
			if (other.credentialRequirementAssignment != null)
				return false;
		} else if (!credentialRequirementAssignment
				.equals(other.credentialRequirementAssignment))
			return false;
		if (formInformation == null)
		{
			if (other.formInformation != null)
				return false;
		} else if (!formInformation.equals(other.formInformation))
			return false;
		if (groupAssignments == null)
		{
			if (other.groupAssignments != null)
				return false;
		} else if (!groupAssignments.equals(other.groupAssignments))
			return false;
		if (groupParams == null)
		{
			if (other.groupParams != null)
				return false;
		} else if (!groupParams.equals(other.groupParams))
			return false;
		if (identityParams == null)
		{
			if (other.identityParams != null)
				return false;
		} else if (!identityParams.equals(other.identityParams))
			return false;
		if (initialEntityState != other.initialEntityState)
			return false;
		if (notificationsConfiguration == null)
		{
			if (other.notificationsConfiguration != null)
				return false;
		} else if (!notificationsConfiguration.equals(other.notificationsConfiguration))
			return false;
		if (publiclyAvailable != other.publiclyAvailable)
			return false;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		
		if (autoAcceptCondition == null)
		{
			if (other.autoAcceptCondition != null)
				return false;
		} else if (!autoAcceptCondition.equals(other.autoAcceptCondition))
			return false;
		
		return true;
	}
}
