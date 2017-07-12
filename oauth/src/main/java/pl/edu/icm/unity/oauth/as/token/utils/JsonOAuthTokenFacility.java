/**
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.OAuthToken;

/**
 * Base for all OAuthToken formatter facilities
 * 
 * @author P.Piernik
 *
 */
public class JsonOAuthTokenFacility
{
	protected ObjectNode toJson(Token t)
	{
		ObjectNode node = Constants.MAPPER.valueToTree(t);

		try
		{
			OAuthToken oauthToken = OAuthToken.getInstanceFromJson(t.getContents());
			((ObjectNode) node).set("contents",
					Constants.MAPPER.valueToTree(oauthToken));
		} catch (IOException e)
		{

		}
		return node;
	}

}
