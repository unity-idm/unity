/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;

import java.util.Optional;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
class OTPCredentialDefinition
{
	final OTPGenerationParams otpParams;
	final String issuerName;
	final int allowedTimeDriftSteps;
	final OTPResetSettings resetSettings;
	final Optional<String> logoURI;
	
	@JsonCreator
	OTPCredentialDefinition(
			@JsonProperty("otpParams") OTPGenerationParams otpParams, 
			@JsonProperty("issuerName") String issuerName, 
			@JsonProperty("allowedTimeDriftSteps") int allowedTimeDriftSeconds,
			@JsonProperty("resetSettings") OTPResetSettings resetSettings,
			@JsonProperty("logoURI") Optional<String> logoUri)
	{
		this.otpParams = otpParams;
		this.issuerName = issuerName;
		this.allowedTimeDriftSteps = allowedTimeDriftSeconds;
		this.resetSettings = resetSettings;
		this.logoURI = logoUri;
	}

	@Override
	public String toString()
	{
		return String.format(
				"OTPCredentialDefinition [otpParams=%s, issuerName=%s, allowedTimeDriftSeconds=%s, logoURI=%s]",
				otpParams, issuerName, allowedTimeDriftSteps, logoURI.orElse(""));
	}
}
