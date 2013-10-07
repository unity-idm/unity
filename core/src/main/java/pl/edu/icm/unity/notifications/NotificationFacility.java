/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations are providing messaging functionality. 
 * 
 * @author K. Benedyczak
 */
public interface NotificationFacility extends DescribedObject
{
	public void validateConfiguration(String configuration) throws WrongArgumentException;
	
	public NotificationChannelInstance getChannel(String configuration);
	
	/**
	 * @return an id of {@link AttributeMetadataProvider}. Attribute with this metadata is resolved in the 
	 * root group and must be of String type.
	 */
	public String getRecipientAddressMetadataKey();
	
}
