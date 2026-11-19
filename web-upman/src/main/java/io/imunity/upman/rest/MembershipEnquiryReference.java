/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import jakarta.ws.rs.BadRequestException;

record MembershipEnquiryReference(String name, boolean autogenerate)
{
	MembershipEnquiryReference
	{
		if (autogenerate && name != null)
			throw new BadRequestException("Property 'name' should be null when property 'autogenerate' is enabled " +
				"inside membershipUpdateEnquiry json object");
	}
}
