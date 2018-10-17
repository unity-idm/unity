/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_5;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

/**
 * Database update helper. If object is update then non empty optional is return;
 * @author P.Piernik
 *
 */
public class UpdateHelperFrom2_5
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperFrom2_5.class);
	
	
	public static Optional<ObjectNode> updateEndpointConfiguration(ObjectNode objContent)
	{
		ObjectNode endpoint = (ObjectNode) objContent.get("configuration");
		ArrayNode authnOptions = (ArrayNode) endpoint.get("authenticationOptions");

		List<String> newAuthns = new ArrayList<>();
		for (JsonNode objNode : authnOptions)
		{
			if (objNode.get("primaryAuthenticator") != null )
			{
				newAuthns.add(objNode.get("primaryAuthenticator").asText());
			}
		}
		authnOptions.removeAll();

		for (String newAuthn : newAuthns)
		{
			authnOptions.add(newAuthn);
		}

		if (!newAuthns.isEmpty())
		{

			log.info("Settings " + objContent.get("name")
					+ " endpoint configuration authenticationOptions to "
					+ newAuthns);
			return Optional.of(objContent);
		}

		return Optional.empty();
	}


	public static void  updateRealm(ObjectNode objContent)
	{

		JsonNode rememberMeFor = (JsonNode) objContent.get("allowForRememberMeDays");

		int rememberMeForDays = rememberMeFor.asInt();
		if (rememberMeForDays > 0)
		{
			log.info("Settings " + objContent.get("name")
					+ " realm configuration rememberMePolicy to "
					+ RememberMePolicy.allowForWholeAuthn.toString());

			objContent.put("rememberMePolicy",
					RememberMePolicy.allowForWholeAuthn.toString());

		} else
		{
			log.info("Settings " + objContent.get("name")
					+ " realm configuration rememberMePolicy to "
					+ RememberMePolicy.disallow.toString()
					+ " and allowForRememberMeDays to 14");

			objContent.remove("allowForRememberMeDays");
			objContent.put("allowForRememberMeDays", "14");
			objContent.put("rememberMePolicy", RememberMePolicy.disallow.toString());
			
		}
	}
}
