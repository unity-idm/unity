/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Specialization of {@link UserRequestState} for {@link RegistrationRequest}s.
 * @author K. Benedyczak
 */
public class RESTRegistrationRequestState extends BasicUserRequestState<RESTRegistrationRequest>
{
	private Long createdEntityId;

	/**
	 * @return for all accepted requests return the entityId of the entity created by the registration. 
	 * Otherwise null.
	 */
	public Long getCreatedEntityId()
	{
		return createdEntityId;
	}
	public void setCreatedEntityId(Long createdEntityId)
	{
		this.createdEntityId = createdEntityId;
	}
}
