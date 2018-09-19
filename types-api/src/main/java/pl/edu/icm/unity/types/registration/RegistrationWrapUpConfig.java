/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

import pl.edu.icm.unity.types.I18nString;

/**
 * Configuration of behavior that can happen in various cases of registration finish.
 * Note that this object can have null values and consumer should handle this by falling up to defaults.
 * @author K. Benedyczak
 */
public class RegistrationWrapUpConfig
{
	public enum TriggeringState {
		DEFAULT(null), 
		AUTO_ACCEPTED("submittedAccepted"), 
		AUTO_REJECTED("submittedRejected"), 
		SUBMITTED("submitted"), 
		INVITATION_EXPIRED("invitationExpired"), 
		INVITATION_MISSING("invitationMissing"), 
		INVITATION_CONSUMED("invitationConsumed"), 
		GENERAL_ERROR("submittedWithError"), 
		PRESET_USER_EXISTS("userExists"), 
		CANCELLED("cancelled"),
		EMAIL_CONFIRMED("elementConfirmed"),
		EMAIL_CONFIRMATION_FAILED("elementConfirmationError");
		
		
		private String urlState;
		
		private TriggeringState(String urlState)
		{
			this.urlState = urlState;
		}

		public String toURLState()
		{
			return urlState;
		}
	}
	
	private TriggeringState state;
	private I18nString title;
	private I18nString info;
	private I18nString redirectCaption;
	private boolean automatic;
	private String redirectURL;

	public RegistrationWrapUpConfig(TriggeringState state)
	{
		this.state = state;
	}

	public RegistrationWrapUpConfig(TriggeringState state, I18nString title, I18nString info,
			I18nString redirectCaption, boolean automatic, String redirectURL)
	{
		this.state = state;
		this.title = title;
		this.info = info;
		this.redirectCaption = redirectCaption;
		this.automatic = automatic;
		this.redirectURL = redirectURL;
	}

	//for JSON
	protected RegistrationWrapUpConfig()
	{
	}
	
	public I18nString getRedirectCaption()
	{
		return redirectCaption;
	}
	public String getRedirectURL()
	{
		return redirectURL;
	}

	public boolean isAutomatic()
	{
		return automatic;
	}

	public I18nString getTitle()
	{
		return title;
	}

	public I18nString getInfo()
	{
		return info;
	}

	public TriggeringState getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "RegistrationWrapUpConfig [state=" + state + ", title=" + title + ", info=" + info
				+ ", redirectCaption=" + redirectCaption + ", automatic=" + automatic + ", redirectURL="
				+ redirectURL + "]";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof RegistrationWrapUpConfig))
			return false;
		RegistrationWrapUpConfig castOther = (RegistrationWrapUpConfig) other;
		return Objects.equals(state, castOther.state) && Objects.equals(title, castOther.title)
				&& Objects.equals(info, castOther.info)
				&& Objects.equals(redirectCaption, castOther.redirectCaption)
				&& Objects.equals(automatic, castOther.automatic)
				&& Objects.equals(redirectURL, castOther.redirectURL);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(state, title, info, redirectCaption, automatic, redirectURL);
	}
}
