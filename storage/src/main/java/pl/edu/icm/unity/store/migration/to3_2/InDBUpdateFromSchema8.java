/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_2;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypesMapper;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * Update db from 3.1 release version (DB schema version 2.8). Drops extracted attributes from identity types.
 * Fixes tokens structure (should be migration in 3.0.0 version actually, as the syntax change was done back then). 
 * Note: for the tokens structure fix there is no JSON dump counterpart. This is because in 2.8 version which was the only one
 * using old PKCS, there was no way to export tokens in JSON dump. This was added in 3.1, but back then schema was already fixed.
 */
@Component
public class InDBUpdateFromSchema8 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema8.class);
	private final TokenRDBMSStore tokensDAO;
	private final Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh");
	
	@Autowired
	public InDBUpdateFromSchema8(TokenRDBMSStore tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 8;
	}
	
	@Override
	public void update() throws IOException
	{
		dropExtractedAttributes();
		updateTokens();
		addGlobalFlag();
	}

	private void addGlobalFlag()
	{
		AttributeTypesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> all = mapper.getAll();
		for (AttributeTypeBean attributeTypeBean : all)
		{
			ObjectNode atType = JsonUtil.parse(attributeTypeBean.getContents());
			atType.put("global", false);
			attributeTypeBean.setContents(JsonUtil.serialize2Bytes(atType));
			mapper.updateByKey(attributeTypeBean);
		}
	}

	private void dropExtractedAttributes()
	{
		IdentityTypesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentityTypesMapper.class);
		List<BaseBean> allInDB = mapper.getAll();
		
		for (BaseBean idTypeBean: allInDB)
		{
			ObjectNode idType = JsonUtil.parse(idTypeBean.getContents());
			idType.remove("extractedAttributes");
			idTypeBean.setContents(JsonUtil.serialize2Bytes(idType));
			mapper.updateByKey(idTypeBean);
		}
	}
	
	void updateTokens() throws IOException
	{
		List<Token> all = tokensDAO.getAll();
		for (Token token : all)
		{
			if (!oauthTokenTypes.contains(token.getType()))
				continue;
			ObjectNode objContent = JsonUtil.parse(token.getContents());

			Optional<ObjectNode> fixed = fixOauthToken(objContent);
			if (fixed.isPresent())
			{
				token.setContents(JsonUtil.serialize2Bytes(fixed.get()));
				tokensDAO.update(token);
				log.info("Updated OAuth token PKCS data to proper 3.x schema: {}", objContent);
			}
		}
	}
	
	private static Optional<ObjectNode> fixOauthToken(ObjectNode objContent)
	{
		if (objContent.has("codeChallenge") || objContent.has("codeChallengeMethod"))
		{
			JsonNode codeChallenge = objContent.remove("codeChallenge");
			JsonNode codeChallengeMethod = objContent.remove("codeChallengeMethod");
			ObjectNode pkcsInfo = objContent.with("pkcsInfo");
			pkcsInfo.set("codeChallenge", codeChallenge);
			pkcsInfo.set("codeChallengeMethod", codeChallengeMethod);
			return Optional.of(objContent);
		}
		return Optional.empty();
	}
}
