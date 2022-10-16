/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_12;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

@Component
public class JsonDumpUpdateFromV17 implements JsonDumpUpdate
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV17.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 17;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		udpateOAuthTokens(contents.withArray("tokens"));
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void udpateOAuthTokens(JsonNode tokensArray) throws IOException
	{
		for (JsonNode tokenNode : tokensArray)
		{
			ObjectNode tokenObj = (ObjectNode) tokenNode;
			String type = tokenObj.get("type").asText();
			if (!UpdateHelperTo17.oauthTokenTypes.contains(type))
				continue;
			ObjectNode objContent = JsonUtil.parse(new String(tokenObj.get("contents").binaryValue()));
			Optional<ObjectNode> fixed = UpdateHelperTo17.fixOauthToken(objContent);
			if (fixed.isPresent())
			{
				tokenObj.remove("contents");
				tokenObj.put("contents", JsonUtil.serialize2Bytes(fixed.get()));
			}
			log.info("Updated OAuth token audience {}", objContent);
		}
	}

}