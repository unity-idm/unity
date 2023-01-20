/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import io.imunity.otp.OTPGenerationParams;

/**
 * Representation of the OTP credential in DB
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
class OTPCredentialDBState
{
	final String secret;
	final OTPGenerationParams otpParams;

	final Date time;
	final boolean outdated;
	final String outdatedReason;
	
	@JsonCreator
	OTPCredentialDBState(
			@JsonProperty("secret") String secret, 
			@JsonProperty("otpParams") OTPGenerationParams otpParams,
			@JsonProperty("time") Date time, 
			@JsonProperty("outdated") boolean outdated, 
			@JsonProperty("outdatedReason") String outdatedReason)
	{
		this.secret = secret;
		this.otpParams = otpParams;
		this.time = time;
		this.outdated = outdated;
		this.outdatedReason = outdatedReason;
	}

	@Override
	public String toString()
	{
		return String.format(
				"OTPCredentialDBState [secret=***, otpParams=%s, time=%s, outdated=%s, outdatedReason=%s]",
				secret, otpParams, time, outdated, outdatedReason);
	}
}
