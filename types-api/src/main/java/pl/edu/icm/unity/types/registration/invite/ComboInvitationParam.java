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
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public class ComboInvitationParam extends InvitationParam
{
	private FormPrefill registrationFormPrefill;
	private FormPrefill enquiryFormPrefill;

	public ComboInvitationParam(String registrationForm, String enquiryForm, Instant expiration)
	{
		super(InvitationType.COMBO, expiration);
		this.enquiryFormPrefill = new FormPrefill(enquiryForm, FormType.ENQUIRY);
		this.registrationFormPrefill = new FormPrefill(registrationForm, FormType.REGISTRATION);
	}

	public ComboInvitationParam(String registrationForm, String enquiryForm, Instant expiration, String contactAddress)
	{
		super(InvitationType.COMBO, expiration, contactAddress);
		this.enquiryFormPrefill = new FormPrefill(enquiryForm, FormType.ENQUIRY);
		this.registrationFormPrefill = new FormPrefill(registrationForm, FormType.REGISTRATION);
	}

	private ComboInvitationParam()
	{
		super(InvitationType.COMBO);
	}

	@JsonCreator
	public ComboInvitationParam(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();

		if (registrationFormPrefill != null)
		{
			ObjectNode putObject = json.putObject("registrationFormPrefill");
			registrationFormPrefill.toJson(putObject);
		}
		if (enquiryFormPrefill != null)
		{
			ObjectNode putObject = json.putObject("enquiryFormPrefill");
			enquiryFormPrefill.toJson(putObject);
		}

		return json;
	}

	protected void fromJson(ObjectNode json)
	{
		registrationFormPrefill = new FormPrefill(FormType.REGISTRATION);
		registrationFormPrefill.fromJson((ObjectNode) json.get("registrationFormPrefill"));
		enquiryFormPrefill = new FormPrefill(FormType.ENQUIRY);
		enquiryFormPrefill.fromJson((ObjectNode) json.get("enquiryFormPrefill"));
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), registrationFormPrefill, enquiryFormPrefill);
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
		ComboInvitationParam other = (ComboInvitationParam) obj;
		return Objects.equals(enquiryFormPrefill, other.enquiryFormPrefill)
				&& Objects.equals(registrationFormPrefill, other.registrationFormPrefill);
	}

	@Override
	public InvitationParam clone()
	{
		return new ComboInvitationParam(this.toJson());
	}

	public FormPrefill getRegistrationFormPrefill()
	{
		return registrationFormPrefill;
	}

	public void setRegistrationFormPrefill(FormPrefill invitationRegistrationForm)
	{
		this.registrationFormPrefill = invitationRegistrationForm;
	}

	public FormPrefill getEnquiryFormPrefill()
	{
		return enquiryFormPrefill;
	}

	public void setEnquiryFormPrefill(FormPrefill invitationEnquiryForm)
	{
		this.enquiryFormPrefill = invitationEnquiryForm;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public void validateUpdate(InvitationParam newInvitationParam) throws WrongArgumentException
	{
		if (newInvitationParam.getType().equals(InvitationType.COMBO))
		{
			assertTypesAreTheSame(newInvitationParam);
			ComboInvitationParam newInv = (ComboInvitationParam) newInvitationParam;

			if (!Objects.equals(getRegistrationFormPrefill().getFormId(),
					newInv.getRegistrationFormPrefill().getFormId()))
				throw new WrongArgumentException("Can not update registration form of an invitation");

			if (!Objects.equals(getEnquiryFormPrefill().getFormId(), newInv.getEnquiryFormPrefill().getFormId()))
				throw new WrongArgumentException("Can not update enquiry form of an invitation");
		} else if (newInvitationParam.getType().equals(InvitationType.ENQUIRY))
		{
			EnquiryInvitationParam newInv = (EnquiryInvitationParam) newInvitationParam;

			if (!Objects.equals(getEnquiryFormPrefill().getFormId(), newInv.getFormPrefill().getFormId()))
				throw new WrongArgumentException("Can not update enquiry form of an invitation");
		} else
		{
			throw new WrongArgumentException("Can not update combo invitation to registration invitation");
		}
	}

	
	
	
	@Override
	public void validate(FormProvider formProvider) throws EngineException
	{
		if (getRegistrationFormPrefill().getFormId() == null || getEnquiryFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}
		EnquiryForm enquiryForm = formProvider.getEnquiryForm(getEnquiryFormPrefill().getFormId());
		assertPrefillMatchesForm(getEnquiryFormPrefill(), enquiryForm);
		RegistrationForm form = formProvider.getRegistrationForm(getRegistrationFormPrefill().getFormId());
		assertPrefillMatchesForm(getRegistrationFormPrefill(), form);
	}

	@Override
	public boolean matchesForm(BaseForm form) throws IllegalFormTypeException
	{
		if (form instanceof EnquiryForm)
		{
			return form.getName().equals(getEnquiryFormPrefill().getFormId());
			
		} else if (form instanceof RegistrationForm)
		{
				return form.getName().equals(getRegistrationFormPrefill().getFormId());	
		} 
		
		return false;
	}

	@Override
	public FormPrefill getPrefillForAutoProcessing()
	{
		return getEnquiryFormPrefill();
	}

	@Override
	public List<FormPrefill> getFormsPrefillData()
	{
		return Arrays.asList(getRegistrationFormPrefill(), getEnquiryFormPrefill());
	}

	@Override
	public FormPrefill getPrefillForForm(BaseForm form) throws EngineException
	{
		if (form instanceof EnquiryForm)
		{
			if (form.getName().equals(getEnquiryFormPrefill().getFormId()))
			{
				return getEnquiryFormPrefill();
			}
			throw new WrongArgumentException("Invitation does not match to enquiry form " + form.getName());

		} else
		{
			if (form.getName().equals(getRegistrationFormPrefill().getFormId()))
			{
				return getRegistrationFormPrefill();
			}

			throw new WrongArgumentException("Invitation does not match to registration form " + form.getName());
		}
	}

	public RegistrationInvitationParam getAsRegistration()
	{
		return RegistrationInvitationParam.builder().withForm(getRegistrationFormPrefill())
				.withContactAddress(getContactAddress()).withExpiration(getExpiration()).build();
	}

	public EnquiryInvitationParam getAsEnquiry(Long resolvedEntity)
	{
		return EnquiryInvitationParam.builder().withForm(getEnquiryFormPrefill()).withEntity(resolvedEntity)
				.withContactAddress(getContactAddress()).withExpiration(getExpiration()).build();
	}


	public static class Builder extends InvitationParam.Builder<Builder>
	{
		private ComboInvitationParam instance;

		public Builder()
		{
			super(new ComboInvitationParam());
			instance = (ComboInvitationParam) super.getInstance();
		}

		public ComboInvitationParam build()
		{
			return instance;
		}

		public Builder withRegistrationForm(FormPrefill formPrefill)
		{
			instance.registrationFormPrefill = formPrefill;
			return this;
		}

		public Builder withEnquiryForm(FormPrefill formPrefill)
		{
			instance.enquiryFormPrefill = formPrefill;
			return this;
		}
	}

	@Override
	public InvitationSendData getSendData()
	{
		return new InvitationSendData(registrationFormPrefill.getFormId(),  registrationFormPrefill.getFormType(), getContactAddress(), getExpiration(),
				registrationFormPrefill.getGroupSelections(), registrationFormPrefill.getMessageParams());
	}
}
