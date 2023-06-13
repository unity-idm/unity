/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Enquiry response, tied to an {@link EnquiryForm}. 
 * Contains only data entered interactively by a user.
 * 
 * @author K. Benedyczak
 */
public class EnquiryResponse extends BaseRegistrationInput
{
	public EnquiryResponse()
	{
	}

	@JsonCreator
	public EnquiryResponse(ObjectNode root)
	{
		super(root);
	}
}
