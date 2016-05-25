/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Specialization of {@link UserRequestState} for {@link RegistrationRequest}s.
 * @author K. Benedyczak
 */
public class RegistrationRequestState extends UserRequestState<RegistrationRequest>
{
	public RegistrationRequestState()
	{
	}

	@JsonCreator
	public RegistrationRequestState(ObjectNode root)
	{
		super(root);
	}

	@Override
	protected RegistrationRequest parseRequestFromJson(ObjectNode root)
	{
		return new RegistrationRequest(root);
	}
}
