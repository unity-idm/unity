/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Implementations are providing messaging functionality. Facility can be configured in 
 * multiple ways and used to produce various notification channels. 
 * Returned {@link NotificationChannelInstance} is used for actual communication.  
 * 
 * @author K. Benedyczak
 */
public interface NotificationFacility extends DescribedObject
{
	void validateConfiguration(String configuration) throws WrongArgumentException;
	
	NotificationChannelInstance getChannel(String configuration);
	
	/**
	 * Returns an address of the entity to be used when sending notifications via the channel.
	 * @param recipient
	 * @param sql
	 * @param preferred if not null then this address should be used if it is available
	 * @return never null, in case no address is found then exception is thrown
	 * @throws EngineException
	 */
	String getAddressForEntity(EntityParam recipient, String preferred, boolean onlyConfirmed) throws EngineException;
	
	/**
	 * Returns an address of the person who filled registration form.
	 * @param currentRequest
	 * @param sql
	 * @return null if not found, address otherwise
	 * @throws EngineException
	 */
	String getAddressForUserRequest(UserRequestState<?> currentRequest) throws EngineException;
	
}
