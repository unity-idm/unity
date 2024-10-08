/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class OTPCredential
{
	public final String secret;
	public final OTPGenerationParams otpParams;

	@JsonCreator
	public OTPCredential(
			@JsonProperty("secret") String secret, 
			@JsonProperty("otpParams") OTPGenerationParams otpParams)
	{
		this.secret = secret;
		this.otpParams = otpParams;
	}

	@Override
	public String toString()
	{
		return String.format("OTPCredential [secret=***, otpParams=%s]", secret, otpParams);
	}
}
