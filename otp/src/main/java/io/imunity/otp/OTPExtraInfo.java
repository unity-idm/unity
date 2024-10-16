/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class OTPExtraInfo
{
	public final Date lastModification;

	@JsonCreator
	public OTPExtraInfo(@JsonProperty("lastModification") Date lastModification)
	{
		this.lastModification = lastModification;
	}
}
