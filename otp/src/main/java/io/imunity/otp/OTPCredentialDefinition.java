/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
class OTPCredentialDefinition
{
	final OTPGenerationParams otpParams;
	final String issuerName;
	final int allowedTimeDriftSteps;
	final OTPResetSettings resetSettings;
	
	@JsonCreator
	OTPCredentialDefinition(
			@JsonProperty("otpParams") OTPGenerationParams otpParams, 
			@JsonProperty("issuerName") String issuerName, 
			@JsonProperty("allowedTimeDriftSteps") int allowedTimeDriftSeconds,
			@JsonProperty("resetSettings") OTPResetSettings resetSettings)
	{
		this.otpParams = otpParams;
		this.issuerName = issuerName;
		this.allowedTimeDriftSteps = allowedTimeDriftSeconds;
		this.resetSettings = resetSettings;
	}

	@Override
	public String toString()
	{
		return String.format(
				"OTPCredentialDefinition [otpParams=%s, issuerName=%s, allowedTimeDriftSeconds=%s]",
				otpParams, issuerName, allowedTimeDriftSteps);
	}
}
