/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * THose parameters are required to generate OTP. Changing them requires update of the user credentials.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class OTPGenerationParams
{
	public final int codeLength;
	public final HashFunction hashFunction;
	public final int timeStepSeconds;

	@JsonCreator
	public OTPGenerationParams(
			@JsonProperty("codeLength") int codeLength, 
			@JsonProperty("hashFunction") HashFunction hashFunction, 
			@JsonProperty("timeStepSeconds") int timeStepSeconds)
	{
		this.codeLength = codeLength;
		this.hashFunction = hashFunction;
		this.timeStepSeconds = timeStepSeconds;
	}

	@Override
	public String toString()
	{
		return String.format("OTPGenerationParams [codeLength=%s, hashFunction=%s, timeStepSeconds=%s]",
				codeLength, hashFunction, timeStepSeconds);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(codeLength, hashFunction, timeStepSeconds);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OTPGenerationParams other = (OTPGenerationParams) obj;
		return codeLength == other.codeLength && hashFunction == other.hashFunction
				&& timeStepSeconds == other.timeStepSeconds;
	}
}
