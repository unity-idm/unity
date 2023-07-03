/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.base.registration.invitation;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationForm;

/**
 * 
 * @author P.Piernik
 *
 */
public class RegistrationInvitationParam extends InvitationParam
{
	private ExpectedIdentity expectedIdentity;
	private FormPrefill formPrefill;
	
	public RegistrationInvitationParam(String formId, Instant expiration)
	{
		super(InvitationType.REGISTRATION, expiration);
		this.formPrefill = new FormPrefill(formId, FormType.REGISTRATION);

	}
	
	public RegistrationInvitationParam(String formId, Instant expiration, String contactAddress)
	{
		super(InvitationType.REGISTRATION, expiration, contactAddress);
		this.formPrefill = new FormPrefill(formId, FormType.REGISTRATION);
	}
	
	private RegistrationInvitationParam() 
	{
		super(InvitationType.REGISTRATION);
	}
	
	@JsonCreator
	public RegistrationInvitationParam(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public ExpectedIdentity getExpectedIdentity()
	{
		return expectedIdentity;
	}

	public void setExpectedIdentity(ExpectedIdentity expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
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
		if (getExpectedIdentity() != null)
			json.putPOJO("expectedIdentity", expectedIdentity);
		if (formPrefill != null)
			formPrefill.toJson(json);
		return json;
	}
	
	protected void fromJson(ObjectNode json)
	{
		JsonNode n = json.get("expectedIdentity");
		if (n != null)
			expectedIdentity = Constants.MAPPER.convertValue(n, ExpectedIdentity.class);
		formPrefill = new FormPrefill(FormType.REGISTRATION);
		formPrefill.fromJson(json);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), expectedIdentity, formPrefill);
	}
	
	@Override
	public InvitationParam clone()
	{
		return new RegistrationInvitationParam(this.toJson());
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
		RegistrationInvitationParam other = (RegistrationInvitationParam) obj;
		return Objects.equals(expectedIdentity, other.expectedIdentity) && Objects.equals(formPrefill, other.formPrefill);
	}
		
	public void validate(FormProvider formProvider) throws EngineException
	{
		if (getFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}

		RegistrationForm form = formProvider.getRegistrationForm(getFormPrefill().getFormId());
		if (!form.isPubliclyAvailable())
			throw new WrongArgumentException("Invitations can be attached to public forms only");
		if (form.getRegistrationCode() != null)
			throw new WrongArgumentException("Invitations can not be attached to forms with a fixed registration code");
		assertPrefillMatchesForm(getFormPrefill(), form);	
	}
	
	public void validateUpdate(InvitationParam newRegistrationInvitationParam) throws WrongArgumentException
	{
		assertTypesAreTheSame(newRegistrationInvitationParam);
		RegistrationInvitationParam newInv = (RegistrationInvitationParam) newRegistrationInvitationParam;
		if (!Objects.equals(getFormPrefill().getFormId(),
				newInv.getFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update registration form of an invitation");
	}

	@Override
	public boolean matchesForm(BaseForm form)
	{
		if (form instanceof RegistrationForm)
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
		if (form == null)
			return getFormPrefill();
		
		if (form instanceof RegistrationForm)
		{
			if (form.getName().equals(getFormPrefill().getFormId()))
			{
				return getFormPrefill();
			}			
		} 
		throw new WrongArgumentException("Invitation does not match to form " + form.getName());
	}
	
	@Override
	public InvitationSendData getSendData()
	{
		return new InvitationSendData(formPrefill.getFormId(),  formPrefill.getFormType(), getContactAddress(), getExpiration(),
				formPrefill.getGroupSelections(), formPrefill.getMessageParams());
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public Builder cloningBuilder()
	{
		return new Builder(this);
	}
	
	public static class Builder extends InvitationParam.Builder<Builder>
	{
		private RegistrationInvitationParam instance ;
		
		public Builder()
		{
			super(new RegistrationInvitationParam());
			instance = (RegistrationInvitationParam) super.getInstance();
			FormPrefill formCommon = new FormPrefill(FormType.REGISTRATION);
			instance.setFormPrefill(formCommon);
		}
		
		public RegistrationInvitationParam build()
		{
			return instance;
		}
		
		public Builder(RegistrationInvitationParam registrationInvitationParam)
		{
			super(new RegistrationInvitationParam(registrationInvitationParam.toJson()));
			instance = registrationInvitationParam;
		}
		
		public Builder withExpectedIdentity(ExpectedIdentity identity)
		{
			instance.expectedIdentity = identity;
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
