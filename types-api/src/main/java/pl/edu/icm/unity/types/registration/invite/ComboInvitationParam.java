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
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.invite.FormPrefill.FormType;

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
	public void validateUpdate(InvitationValidator validator, InvitationParam toUpdate) throws EngineException
	{
		validator.validateUpdate(this, toUpdate);
	}

	public void validate(InvitationValidator validator) throws EngineException
	{
		validator.validate(this);
	}

	@Override
	public void send(InvitationSender sender, String code) throws EngineException
	{
		sender.send(
				RegistrationInvitationParam.builder().withContactAddress(this.getContactAddress())
						.withExpiration(this.getExpiration()).withForm(this.getRegistrationFormPrefill()).build(),
				code);
	}

	@Override
	public boolean matchForm(BaseForm form)
	{
		if (form instanceof EnquiryForm)
		{
			if (form.getName().equals(getEnquiryFormPrefill().getFormId()))
			{
				return true;
			}
		} else
		{
			if (form.getName().equals(getRegistrationFormPrefill().getFormId()))
			{
				return true;
			}
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
			throw new WrongArgumentException("Invitation not match to enquiry form " + form.getName());

		} else
		{
			if (form.getName().equals(getRegistrationFormPrefill().getFormId()))
			{
				return getRegistrationFormPrefill();
			}

			throw new WrongArgumentException("Invitation not match to registration form " + form.getName());
		}
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
}
