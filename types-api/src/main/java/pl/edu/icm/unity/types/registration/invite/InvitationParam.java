/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.util.AntPathMatcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;

/**
 * Base data of invitation parameter. It is extracted as we have two ways to represent attributes:
 * one simple for JSON API and one with resolved Attribute for the backend.
 * @author Krzysztof Benedyczak
 */
public abstract class InvitationParam
{
	public static enum InvitationType { ENQUIRY, REGISTRATION, COMBO};
		
	private InvitationType type;
	private Instant expiration;
	private String contactAddress;
	private Optional<Long> inviterEntity;
	
	protected InvitationParam(InvitationType type) 
	{
		this.type = type;
		this.inviterEntity = Optional.empty();
	}
	
	public InvitationParam(InvitationType type, Instant expiration, String contactAddress)
	{
		this(type, expiration);
		this.contactAddress = contactAddress;
		this.inviterEntity = Optional.empty();
	}

	public InvitationParam(InvitationType type, Instant expiration)
	{
		this.type = type;
		this.expiration = expiration;
		this.inviterEntity = Optional.empty();
	}

	@JsonCreator
	public InvitationParam(ObjectNode json)
	{
		fromJsonBase(json);
	}
	
	public InvitationType getType()
	{
		return type;
	}

	public void setType(InvitationType type)
	{
		this.type = type;
	}

	public Instant getExpiration()
	{
		return expiration;
	}

	public String getContactAddress()
	{
		return contactAddress;
	}

	public Optional<Long> getInviterEntity()
	{
		return inviterEntity;
	}

	public void setInviterEntity(Optional<Long> inviterEntity)
	{
		this.inviterEntity = inviterEntity;
	}
	
	public abstract void validateUpdate(InvitationParam toUpdate) throws EngineException;
	public abstract void validate(FormProvider formProvider) throws EngineException;
	public abstract boolean matchesForm(BaseForm form) throws IllegalFormTypeException;
	
	/**
	 * Get prefill data associated with given form
	 * @throws EngineException
	 */
	public abstract FormPrefill getPrefillForForm(BaseForm form) throws EngineException;

	/**
	 * Get prefill data used in auto processing invitation action.  
	 * @return
	 */
	public abstract FormPrefill getPrefillForAutoProcessing();
	public abstract List<FormPrefill> getFormsPrefillData();
	public abstract InvitationSendData getSendData() throws EngineException;
	
	protected void assertTypesAreTheSame(InvitationParam newInvitationParam)
			throws WrongArgumentException
	{
		if (!Objects.equals(type, newInvitationParam.getType()))
			throw new WrongArgumentException("Can not update invitation, the types of invitations are not the same");
	}
	
	protected void assertPrefillMatchesForm(FormPrefill invitation, BaseForm form)
	{
		assertIdentitiesMatch(invitation, form);
		assertAttributesMatch(invitation, form);
		assertPrefilledGroupsMatch(invitation, form);
	}

	private void assertPrefilledGroupsMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getGroupParams().size() - 1;
		invitation.getGroupSelections().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled group index " + index + " has no corresponding group parameter in the form");
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(index);
			if (!groupRegistrationParam.isMultiSelect() && param.getEntry().getSelectedGroups().size() > 1)
				throw new IllegalArgumentException("Prefilled group with index " + index
						+ " has multiple groups selected while only one is allowed.");
			for (String prefilledGroup : param.getEntry().getSelectedGroups())
				if (!groupMatches(prefilledGroup, groupRegistrationParam.getGroupPath()))
					throw new IllegalArgumentException("Prefilled group " + prefilledGroup
							+ " is not matching allowed groups spec " + groupRegistrationParam.getGroupPath());
		});
	}
	private boolean groupMatches(String group, String pattern)
	{
		AntPathMatcher matcher = new AntPathMatcher();
		return matcher.match(pattern, group);
	}
	
	private void assertAttributesMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getAttributeParams().size() - 1;
		invitation.getAttributes().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled attribute index " + index + " has no corresponding attribute parameter in the form");
			AttributeRegistrationParam attributeRegistrationParam = form.getAttributeParams().get(index);
			if (!attributeRegistrationParam.getAttributeType().equals(param.getEntry().getName()))
				throw new IllegalArgumentException("Prefilled attribute at index " + index
						+ " has other attribute then the one in the form: " + param.getEntry().getName()
						+ " while expected " + attributeRegistrationParam.getAttributeType());
		});
	}

	private void assertIdentitiesMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getIdentityParams().size() - 1;
		invitation.getIdentities().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled identity index " + index + " has no corresponding identity parameter in the form");
			IdentityRegistrationParam identityRegistrationParam = form.getIdentityParams().get(index);
			if (!identityRegistrationParam.getIdentityType().equals(param.getEntry().getTypeId()))
				throw new IllegalArgumentException("Prefilled identity index " + index
						+ " has different type then the form's param: " + param.getEntry().getTypeId()
						+ ", while expected: " + identityRegistrationParam.getIdentityType());
		});
	}
	
	@JsonIgnore
	public boolean isExpired()
	{
		return Instant.now().isAfter(getExpiration());
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = Constants.MAPPER.createObjectNode();
		
		json.put("type", getType().toString());
		json.put("expiration", getExpiration().toEpochMilli());
		if (getContactAddress() != null)
			json.put("contactAddress", getContactAddress());
		
		getInviterEntity().ifPresent(inviter -> json.put("inviter", inviter));
		
		return json;
	}
	
	private void fromJsonBase(ObjectNode json)
	{
		
		JsonNode n;
		n=json.get("type");
		if (n != null && !n.isNull())
		{
			type = InvitationType.valueOf(json.get("type").asText());	
		}else
		{
			type = InvitationType.REGISTRATION;	
		}
			
		expiration = Instant.ofEpochMilli(json.get("expiration").asLong());
		contactAddress = JsonUtil.getNullable(json, "contactAddress");
		n=json.get("inviter");
		inviterEntity = Optional.ofNullable(n != null && !n.isNull() ? n.asLong() : null);
			
		
	}
	
	@Override
	public String toString()
	{
		return "InvitationParam [type=" + type +  ", expiration=" + expiration
				+ ", contactAddress=" + contactAddress + ", inviter=" + (inviterEntity.isEmpty() ? "" : inviterEntity.get());
	} 

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof InvitationParam))
			return false;
		InvitationParam castOther = (InvitationParam) other;
		return  Objects.equals(type, castOther.type) &&
				Objects.equals(expiration, castOther.expiration)
				&& Objects.equals(contactAddress, castOther.contactAddress)
				&& Objects.equals(inviterEntity, castOther.inviterEntity);
				
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(expiration, contactAddress, inviterEntity);
	}

	protected static class Builder<T extends Builder<?>> {
	
		private InvitationParam instance;

		protected Builder(InvitationParam aInstance)
		{
			instance = aInstance;
		}

		protected InvitationParam getInstance()
		{
			return instance;
		}
			
		@SuppressWarnings("unchecked")
		public  T  withExpiration(Instant expiration)
		{
			instance.expiration = expiration;
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public  T  withContactAddress(String contactAddress)
		{
			instance.contactAddress = contactAddress;
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public  T  withInviter(Long inviter)
		{
			instance.inviterEntity = Optional.ofNullable(inviter);
			return (T) this;
		}
	}
}
