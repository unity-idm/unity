/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.DescribedObject;

/**
 * Represent facility used for map object to json ObjectNode
 * @author P.Piernik
 *
 */
public interface JsonFormatterFacility extends DescribedObject
{
	ObjectNode toJson(Object o);
}
