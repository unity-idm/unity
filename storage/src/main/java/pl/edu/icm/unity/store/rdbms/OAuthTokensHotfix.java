/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.rdbms;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;

/**
 * Changes all OAuth tokens which contains 	
 * codeChallenge or codeChallengeMethod elements so that those are properly wrapped in pkcsInfo element. 
 * This is fixing problem introduced by 3.0 change which was not accompanied by migration.
 *  
 * To be replaced in 3.2.0 with regular migration.
 * 
 */
@Component
public class OAuthTokensHotfix 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, OAuthTokensHotfix.class);
	private final TokenRDBMSStore tokensDAO;
	private final Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh");

	@Autowired
	public OAuthTokensHotfix(TokenRDBMSStore tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}

	void updateTokens() throws IOException
	{
		List<Token> all = tokensDAO.getAll();
		for (Token token : all)
		{
			if (!oauthTokenTypes.contains(token.getType()))
				continue;
			ObjectNode objContent = JsonUtil.parse(token.getContents());

			if (objContent.has("codeChallenge") || objContent.has("codeChallengeMethod"))
			{
				JsonNode codeChallenge = objContent.remove("codeChallenge");
				JsonNode codeChallengeMethod = objContent.remove("codeChallengeMethod");
				ObjectNode pkcsInfo = objContent.with("pkcsInfo");
				pkcsInfo.set("codeChallenge", codeChallenge);
				pkcsInfo.set("codeChallengeMethod", codeChallengeMethod);
				token.setContents(JsonUtil.serialize2Bytes(objContent));
				tokensDAO.update(token);
				log.info("Updated OAuth token PKCS data to proper 3.x schema: {}", objContent);
			}
		}
	}
}
