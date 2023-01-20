/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OTPResetSettings
{
	public enum ConfirmationMode 
	{
		EMAIL(true, false), 
		MOBILE(false, true), 
		EMAIL_AND_MOBILE(true, true), 
		EMAIL_OR_MOBILE(true, true);
		
		private boolean email;
		private boolean mobile;
		
		private ConfirmationMode(boolean email, boolean mobile)
		{
			this.email = email;
			this.mobile = mobile;
		}

		public boolean isEmail()
		{
			return email;
		}

		public boolean isMobile()
		{
			return mobile;
		}

		public boolean requiresMobileConfirmation()
		{
			return this == MOBILE || this == EMAIL_AND_MOBILE;
		}

		public boolean requiresEmailConfirmation()
		{
			return this == EMAIL || this == EMAIL_AND_MOBILE;
		}
	}
	
	public final boolean enabled;
	public final int codeLength;
	public final String emailSecurityCodeMsgTemplate;
	public final String mobileSecurityCodeMsgTemplate;
	public final ConfirmationMode confirmationMode;
	
	@JsonCreator
	public OTPResetSettings(
			@JsonProperty("enabled") boolean enabled, 
			@JsonProperty("codeLength") int codeLength, 
			@JsonProperty("emailSecurityCodeMsgTemplate") String emailSecurityCodeMsgTemplate,
			@JsonProperty("mobileSecurityCodeMsgTemplate") String mobileSecurityCodeMsgTemplate, 
			@JsonProperty("confirmationMode") ConfirmationMode confirmationMode)
	{
		this.enabled = enabled;
		this.codeLength = codeLength;
		this.emailSecurityCodeMsgTemplate = emailSecurityCodeMsgTemplate;
		this.mobileSecurityCodeMsgTemplate = mobileSecurityCodeMsgTemplate;
		this.confirmationMode = confirmationMode;
	}
	
	
	

}
