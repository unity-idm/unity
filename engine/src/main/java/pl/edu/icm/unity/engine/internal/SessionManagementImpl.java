/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link SessionManagement}
 * @author K. Benedyczak
 */
@Component
public class SessionManagementImpl implements SessionManagement
{
	public static final String SESSION_TOKEN_TYPE = "session";
	private TokensManagement tokensManagement;
	private ObjectMapper mapper;
	
	@Autowired
	public SessionManagementImpl(TokensManagement tokensManagement, ObjectMapper mapper)
	{
		this.tokensManagement = tokensManagement;
		this.mapper = mapper;
	}

	@Override
	public String createSession(LoginSession session)
	{
		UUID randomid = UUID.randomUUID();
		String id = randomid.toString();
		try
		{
			tokensManagement.addToken(SESSION_TOKEN_TYPE, id, new EntityParam(session.getEntityId()), 
					getTokenContents(session), session.getStarted(), session.getExpires(), null);
		} catch (Exception e)
		{
			throw new InternalException("Can't create a new session", e);
		}
		return id;
	}

	@Override
	public void updateSessionExpirtaion(String id, Date newExpires) throws WrongArgumentException
	{
		tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, newExpires, null, null);
	}

	@Override
	public void updateSessionAttributes(String id, String attributeKey, String attributeValue) 
			throws WrongArgumentException
	{
		Object transaction = tokensManagement.startTokenTransaction();
		try
		{
			Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id, transaction);
			LoginSession session = token2session(token);
			if (attributeValue != null)
				session.getSessionData().put(attributeKey, attributeValue);
			else
				session.getSessionData().remove(attributeKey);
			byte[] contents = getTokenContents(session);
			tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents, transaction);
			tokensManagement.commitTokenTransaction(transaction);
		} finally
		{
			tokensManagement.closeTokenTransaction(transaction);
		}
	}

	@Override
	public void removeSession(String id)
	{
		try
		{
			tokensManagement.removeToken(SESSION_TOKEN_TYPE, id, null);
		} catch (WrongArgumentException e)
		{
			//not found - ok
		}
	}

	@Override
	public LoginSession getSession(String id) throws WrongArgumentException
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id, null);
		return token2session(token);
	}

	private LoginSession token2session(Token token)
	{
		LoginSession session = new LoginSession(token.getValue(), token.getCreated(), token.getExpires(), 
				token.getOwner(), null);
		
		ObjectNode main;
		try
		{
			main = mapper.readValue(token.getContents(), ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		session.setRealm(main.get("realm").asText());
		
		Map<String, String> attrs = new HashMap<String, String>(); 
		ObjectNode attrsJson = (ObjectNode) main.get("attributes");
		Iterator<String> fNames = attrsJson.fieldNames();
		while (fNames.hasNext())
		{
			String attrName = fNames.next();
			attrs.put(attrName, attrsJson.get(attrName).asText());
		}
		session.setSessionData(attrs);
		return session;
	}
	
	private byte[] getTokenContents(LoginSession session)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("realm", session.getRealm());
		
		ObjectNode attrsJson = main.putObject("attributes");
		for (Map.Entry<String, String> a: session.getSessionData().entrySet())
			attrsJson.put(a.getKey(), a.getValue());

		try
		{
			return mapper.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

}
