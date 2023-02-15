/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class OTPCredentialDefinition
{
	public final OTPGenerationParams otpParams;
	public final String issuerName;
	public final int allowedTimeDriftSteps;
	public final OTPResetSettings resetSettings;
	public final Optional<String> logoURI;
	
	@JsonCreator
	public OTPCredentialDefinition(
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
