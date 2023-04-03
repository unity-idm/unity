/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;

/**
 * Handles import/export of tokens table.
 * @author P.Piernik
 */
@Component
public class TokensIE extends AbstractIEBase<Token>
{
	public static final String TOKEN_OBJECT_TYPE = "tokens";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, TokensIE.class);	
	private TokenDAO dbTokens;
	
	@Autowired
	public TokensIE(TokenDAO dbTokens, ObjectMapper objectMapper)
	{
		super(9, TOKEN_OBJECT_TYPE, objectMapper);
		this.dbTokens = dbTokens;
	}
	
	@Override
	protected List<Token> getAllToExport()
	{
		return dbTokens.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(Token exportedObj)
	{
		return jsonMapper.valueToTree(exportedObj);
	}

	@Override
	protected void createSingle(Token toCreate)
	{
		dbTokens.create(toCreate);
	}

	@Override
	protected Token fromJsonSingle(ObjectNode src)
	{
		try {
			return jsonMapper.treeToValue(src, Token.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize Token object:", e);
		}
		return null;
	}
}








