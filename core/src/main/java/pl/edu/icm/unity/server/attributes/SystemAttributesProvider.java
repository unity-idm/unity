/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.attributes;

import java.util.List;

import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implementations (must be Spring components) provide additional system attribute types, defined by extensions. 
 * Those types are added to the database at startup (only if undefined). 
 * @author K. Benedyczak
 */
public interface SystemAttributesProvider
{
	List<AttributeType> getSystemAttributes();
}
