/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupSelection;

public class EnquiryInvitationParam extends InvitationParam
{
	private Long entity;
	private FormPrefill formPrefill;
	
	public EnquiryInvitationParam(String formId, Instant expiration)
	{
		super(InvitationType.ENQUIRY, expiration);
		this.formPrefill = new FormPrefill(formId, FormType.ENQUIRY);
	}
	
	public EnquiryInvitationParam(String formId, Instant expiration, String contactAddress)
	{
		super(InvitationType.ENQUIRY, expiration, contactAddress);
		this.formPrefill = new FormPrefill(formId, FormType.ENQUIRY);
	}
	
	
	private EnquiryInvitationParam() 
	{
		super(InvitationType.ENQUIRY);
	}
	
	@JsonCreator
	public EnquiryInvitationParam(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public Long getEntity()
	{
		return entity;
	}

	public void setEntity(Long entity)
	{
		this.entity = entity;
	}

	public FormPrefill getFormPrefill()
	{
		return formPrefill;
	}

	public void setFormPrefill(FormPrefill formPrefil)
	{
		this.formPrefill = formPrefil;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();
		if (getEntity() != null)
			json.put("entity", entity);
		if (formPrefill != null)
			formPrefill.toJson(json);
		return json;
	}
	
	protected void fromJson(ObjectNode json)
	{
		JsonNode n = json.get("entity");
		if (n != null)
			entity = n.asLong();
		formPrefill = new FormPrefill(FormType.ENQUIRY);
		formPrefill.fromJson(json);
		
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), entity, formPrefill);
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnquiryInvitationParam other = (EnquiryInvitationParam) obj;
		return Objects.equals(entity, other.entity) && Objects.equals(formPrefill, other.formPrefill);
	}
	
	@Override
	public InvitationParam clone()
	{
		return new EnquiryInvitationParam(this.toJson());
	}
	
	public void validateUpdate(InvitationParam newInvitationParam)
			throws WrongArgumentException
	{
		assertTypesAreTheSame(newInvitationParam);
		EnquiryInvitationParam newInv = (EnquiryInvitationParam) newInvitationParam;
		if (!Objects.equals(getFormPrefill().getFormId(),
				newInv.getFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update enquiry form of an invitation");
	}
	
	public void validate(FormProvider formProvider) throws EngineException
	{
		if (getFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}
		EnquiryForm enquiryForm = formProvider.getEnquiryForm(getFormPrefill().getFormId());
		assertPrefillMatchesForm(getFormPrefill(), enquiryForm);
	}
	
	@Override
	public boolean matchesForm(BaseForm form)
	{
		if (form instanceof EnquiryForm)
		{
			if (form.getName().equals(getFormPrefill().getFormId()))
			{
				return true;
			}
		} 
		return false;
	}

	@Override
	public FormPrefill getPrefillForAutoProcessing()
	{
		return getFormPrefill();
	}
	
	@Override
	public List<FormPrefill> getFormsPrefillData()
	{
		return Arrays.asList(getFormPrefill());
	}
	
	@Override
	public FormPrefill getPrefillForForm(BaseForm form) throws EngineException
	{
		if (form instanceof EnquiryForm)
		{
			if (form.getName().equals(getFormPrefill().getFormId()))
			{
				return getFormPrefill();
			}			
		} 
		throw new WrongArgumentException("Invitation does not match to form " + form.getName());
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public Builder cloningBuilder()
	{
		return new Builder(this);
	}
	
	@Override
	public InvitationSendData getSendData() throws EngineException
	{
		if (getEntity() == null)
			throw new WrongArgumentException("The invitation has no entity configured");
		
		return new InvitationSendData(formPrefill.getFormId(),  formPrefill.getFormType(), getContactAddress(), getExpiration(),
				formPrefill.getGroupSelections(), formPrefill.getMessageParams());
	}
	
	public static class Builder extends InvitationParam.Builder<Builder>
	{
		private EnquiryInvitationParam instance;

		public Builder()
		{
			super(new EnquiryInvitationParam());
			instance = (EnquiryInvitationParam) super.getInstance();
			FormPrefill formCommon = new FormPrefill(FormType.ENQUIRY);
			instance.setFormPrefill(formCommon);	
		}
		
		public Builder(EnquiryInvitationParam enquiryInvitationParam)
		{
			super(new EnquiryInvitationParam(enquiryInvitationParam.toJson()));
			instance = enquiryInvitationParam;
		}
			
		public EnquiryInvitationParam build()
		{
			return instance;
		}
		
		public Builder withEntity(Long entity)
		{
			instance.entity = entity;
			return this;
		}
		
		public Builder withForm(FormPrefill invitationFormCommon)
		{
			instance.formPrefill = invitationFormCommon;
			return this;
		}
		
		public Builder withForm(String formId)
		{
			instance.formPrefill.setFormId(formId);
			return this;
		}
		
		public Builder withAttribute(Attribute attribute, PrefilledEntryMode mode)
		{
			int idx = instance.getFormPrefill().getAttributes().size();
			instance.getFormPrefill().getAttributes().put(idx, new PrefilledEntry<>(attribute, mode));
			return this;
		}

		public Builder withGroup(String group, PrefilledEntryMode mode)
		{
			int idx = instance.getFormPrefill().getGroupSelections().size();
			instance.getFormPrefill().getGroupSelections().put(idx, new PrefilledEntry<>(new GroupSelection(group), mode));
			return this;
		}

		public Builder withGroups(List<String> groups, PrefilledEntryMode mode)
		{
			int idx = instance.getFormPrefill().getGroupSelections().size();
			instance.getFormPrefill().getGroupSelections().put(idx, new PrefilledEntry<>(new GroupSelection(groups), mode));
			return this;
		}

		public Builder withAllowedGroups(List<String> groups)
		{
			int idx = instance.getFormPrefill().getAllowedGroups().size();
			instance.getFormPrefill().getAllowedGroups().put(idx, new GroupSelection(groups));
			return this;
		}

		public Builder withIdentity(IdentityParam identity, PrefilledEntryMode mode)
		{
			int idx = instance.getFormPrefill().getIdentities().size();
			instance.getFormPrefill().getIdentities().put(idx, new PrefilledEntry<>(identity, mode));
			return this;
		}
		
	}
}
