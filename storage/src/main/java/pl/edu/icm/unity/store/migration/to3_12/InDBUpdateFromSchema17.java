/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_12;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

@Component
public class InDBUpdateFromSchema17 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema17.class);

	private final TokenRDBMSStore tokensDAO;
	
	@Autowired
	public InDBUpdateFromSchema17(TokenRDBMSStore tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 17;
	}

	@Override
	public void update() throws IOException
	{
		updateOAuthTokens();
	}

	void updateOAuthTokens() throws IOException
	{
		List<Token> all = tokensDAO.getAll();
		for (Token token : all)
		{
			if (!UpdateHelperTo17.oauthTokenTypes.contains(token.getType()))
				continue;
			ObjectNode objContent = JsonUtil.parse(token.getContents());

			Optional<ObjectNode> fixed = UpdateHelperTo17.fixOauthToken(objContent);
			if (fixed.isPresent())
			{
				token.setContents(JsonUtil.serialize2Bytes(fixed.get()));
				tokensDAO.update(token);
				log.info("Updated OAuth token audience {}", objContent);
			}
		}
	}
}
