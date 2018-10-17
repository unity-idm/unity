/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Represent facility used for map contents of {@link Token} objects (which is basically an opaque
 * byte array for the engine) to some meaningful JSON representation, useful especially 
 * when presenting tokens to outside world, e.g. via REST interface.
 * @author P.Piernik
 *
 */
public interface TokenContentsJsonSerializer extends DescribedObject
{
	JsonNode toJson(byte[] rawValue) throws EngineException;
}
