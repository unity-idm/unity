/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OTPExtraInfo
{
	public final Date lastModification;

	@JsonCreator
	public OTPExtraInfo(@JsonProperty("lastModification") Date lastModification)
	{
		this.lastModification = lastModification;
	}
}
