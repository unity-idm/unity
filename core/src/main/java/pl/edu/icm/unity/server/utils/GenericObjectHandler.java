/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Each component of the system which inserts some values into generic objects table
 * must provide a matching implementation of this interface, i.e. there must be one implementation
 * per generic object type.
 * <p>
 * The implementation is responsible for updating imported contents to the latest system schema.
 * @author K. Benedyczak
 */
public interface GenericObjectHandler
{
	public String getSupportedType();
	
	public byte[] updateBeforeImport(String type, String subType, String name, JsonNode contents) 
			throws JsonProcessingException;
}
