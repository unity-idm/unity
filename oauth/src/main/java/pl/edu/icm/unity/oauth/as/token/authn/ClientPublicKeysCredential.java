/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.token.JwksParseUtils;

public class ClientPublicKeysCredential
{
	public ObjectNode getSerializedConfiguration()
	{
		return Constants.MAPPER.createObjectNode();
	}

	public void setSerializedConfiguration(ObjectNode root) throws InternalException
	{
	}

	public String prepareForStorage(String rawJwks) throws InternalException
	{
		if (rawJwks == null || rawJwks.isBlank())
			return "";
		JwksParseUtils.parseRequired(rawJwks, "Invalid JWK Set");
		return rawJwks;
	}
}
