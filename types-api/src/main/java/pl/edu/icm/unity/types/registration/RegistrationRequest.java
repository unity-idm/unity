/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Registration request, tied to a registration form contains data collected
 * during registration process. This data can be entered by the user in UI,
 * taken from external IdP or possibly from other sources (e.g. a DN can be
 * taken from client-authenticated TLS).
 * 
 * @author K. Benedyczak
 */
public class RegistrationRequest extends BaseRegistrationInput
{
	public RegistrationRequest()
	{
	}

	@JsonCreator
	public RegistrationRequest(ObjectNode root)
	{
		super(root);
	}
}
