/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_9;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

class UpdateHelperTo3_9
{
	static Optional<ObjectNode> migrateExternalSignupSpec(ObjectNode objContent)
	{
		JsonNode signupSpec = objContent.get("ExternalSignupSpec");
		if (signupSpec == null || signupSpec.isNull())
			return Optional.empty();
		
		ArrayNode specs = signupSpec.withArray("specs");
		if (specs.isEmpty())
			return Optional.empty();
		
		List<ObjectNode> newSpecs = new ArrayList<>();
		Iterator<JsonNode> iter = specs.iterator();
		while (iter.hasNext())
		{
			JsonNode spec = iter.next();
			if (!spec.isObject() && spec.isTextual())
			{
				iter.remove();
				
				String specText = spec.asText();
				String[] specArray = specText.split("\\.");
				if (specArray.length != 2)
					throw new IllegalStateException("Invalid formst of ExternalSignupSpec: " + objContent.toString());
				String authenticatorKey = specArray[0];
				String optionKey = specArray[1];
				
				ObjectNode newSpec = Constants.MAPPER.createObjectNode();
				newSpec.put("authenticatorKey", authenticatorKey);
				newSpec.put("optionKey", optionKey);
				newSpecs.add(newSpec);
				
			}
		}
		
		if (newSpecs.isEmpty())
			return Optional.empty();
		
		specs.addAll(newSpecs);
		return Optional.of(objContent);
	}
}


