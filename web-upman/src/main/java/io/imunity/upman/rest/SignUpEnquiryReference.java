/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import jakarta.ws.rs.BadRequestException;

record SignUpEnquiryReference(String name, boolean autogenerate)
{
	SignUpEnquiryReference
	{
		if (autogenerate && name != null)
			throw new BadRequestException("Property 'name' should be null when property 'autogenerate' is enabled " +
				"inside signUpEnquiry json object");
	}
}
