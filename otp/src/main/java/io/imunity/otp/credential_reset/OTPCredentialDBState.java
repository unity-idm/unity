/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.credential_reset;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.imunity.otp.OTPGenerationParams;

import java.util.Date;

/**
 * Representation of the OTP credential in DB
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class OTPCredentialDBState
{
	public final String secret;
	public final OTPGenerationParams otpParams;

	public final Date time;
	public final boolean outdated;
	public final String outdatedReason;
	
	@JsonCreator
	public OTPCredentialDBState(
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
