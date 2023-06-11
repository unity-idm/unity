/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class OAuthTokenController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, OAuthTokenController.class);

	private SecuredTokensManagement tokenMan;
	private EntityManagement entityManagement;
	private EndpointManagement endpointMan;
	private MessageSource msg;

	private final OAuthAccessTokenRepository accessTokenRepository;
	private final OAuthRefreshTokenRepository refreshTokenRepository;


	@Autowired
	OAuthTokenController(MessageSource msg, OAuthAccessTokenRepository oauthTokenRepository,
			OAuthRefreshTokenRepository refreshTokenRepository,
			SecuredTokensManagement tokensManagement,
			EntityManagement entityManagement, EndpointManagement endpointMan)
	{
		this.msg = msg;
		this.accessTokenRepository = oauthTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenMan = tokensManagement;
		this.entityManagement = entityManagement;
		this.endpointMan = endpointMan;
	}

	private Optional<Endpoint> getEndpoint(String name)
	{
		try
		{
			return endpointMan.getEndpoints().stream()
					.filter(e -> e.getTypeId().equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName())
							&& e.getName().equals(name))
					.findFirst();
		} catch (EngineException e)
		{
			return Optional.empty();
		}

	}

	private String getIssuerUri(Endpoint endpoint)
	{

		try
		{
			Properties raw = new Properties();
			raw.load(new StringReader(endpoint.getConfiguration().getConfiguration()));
			OAuthASProperties oauthProperties = new OAuthASProperties(raw);
			return oauthProperties.getIssuerName();
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth idp service", e);
		}
	}

	public Collection<OAuthTokenBean> getOAuthTokens(String serviceName) throws ControllerException
	{
		Optional<Endpoint> endpoint = getEndpoint(serviceName);
		if (!endpoint.isPresent())
		{
			return Collections.emptyList();
		}

		try
		{
			List<Token> tokens = getTokens();
			return tokens.stream().map(t -> new OAuthTokenBean(t, msg, establishOwner(t)))
					.filter(t -> t.getServerId().equals(getIssuerUri(endpoint.get())))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("OAuthTokenController.getTokensError", serviceName), e);
		}

	}

	private String establishOwner(Token token)
	{
		long ownerId = token.getOwner();
		String idLabel = "[" + ownerId + "]";
		String attrNameValue = getDisplayedName(ownerId);
		return attrNameValue != null ? idLabel + " " + attrNameValue : idLabel;
	}

	private String getDisplayedName(long owner)
	{
		try
		{
			return entityManagement.getEntityLabel(new EntityParam(owner));
		} catch (Exception e)
		{
			log.debug("Can't get user's displayed name attribute for " + owner, e);
		}
		return null;
	}

	private List<Token> getTokens() throws EngineException
	{
		List<Token> tokens = new ArrayList<>();
		tokens.addAll(accessTokenRepository.getAllAccessTokens());
		tokens.addAll(refreshTokenRepository.getAllRefreshTokens());
		return tokens;
	}

	public void removeToken(OAuthTokenBean item) throws ControllerException
	{
		try
		{
			tokenMan.removeToken(item.getRealType(), item.getId());
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("OAuthTokenController.removeTokenError", item.getId()), e);
		}

	}

}