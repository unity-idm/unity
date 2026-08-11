/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationGrantError;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;

import jakarta.ws.rs.core.Response;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * RFC 8628 §3.4/§3.5: handles device_code polling on the token endpoint.
 */
class DeviceCodeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceCodeHandler.class);

	private final DeviceCodeRepository deviceCodeRepository;
	private final TransactionalRunner tx;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenRepository;
	private final OAuthTokenStatisticPublisher statisticsPublisher;
	private final OAuthASProperties config;
	private final TokenService tokenService;

	DeviceCodeHandler(DeviceCodeRepository deviceCodeRepository, TransactionalRunner tx,
			AccessTokenFactory accessTokenFactory, OAuthAccessTokenRepository accessTokenDAO,
			OAuthRefreshTokenRepository refreshTokenRepository, OAuthTokenStatisticPublisher statisticsPublisher,
			OAuthASProperties config, TokenService tokenService)
	{
		this.deviceCodeRepository = deviceCodeRepository;
		this.tx = tx;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokenDAO = accessTokenDAO;
		this.refreshTokenRepository = refreshTokenRepository;
		this.statisticsPublisher = statisticsPublisher;
		this.config = config;
		this.tokenService = tokenService;
	}

	Response handleDeviceCodeGrant(String deviceCode, String acceptHeader) throws EngineException
	{
		try
		{
			return tx.runInTransactionRetThrowing(() -> handleInTransaction(deviceCode, acceptHeader));
		} catch (OAuthErrorException e)
		{
			return e.response;
		}
	}

	private Response handleInTransaction(String deviceCodeValue, String acceptHeader) throws OAuthErrorException
	{
		Optional<Token> tokenOpt = deviceCodeRepository.getByDeviceCode(deviceCodeValue);
		if (tokenOpt.isEmpty())
		{
			statisticsPublisher.reportFailAsLoggedClient();
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
		}

		Token deviceToken = tokenOpt.get();
		OAuthToken parsedToken = BaseOAuthResource.parseInternalToken(deviceToken);

		if (deviceToken.getExpires() != null && deviceToken.getExpires().before(new Date()))
		{
			deviceCodeRepository.remove(deviceCodeValue);
			statisticsPublisher.reportFail(parsedToken.getClientUsername(), parsedToken.getClientName());
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(DeviceAuthorizationGrantError.EXPIRED_TOKEN, null));
		}

		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		if (loginSession != null && parsedToken.getClientId() != loginSession.getEntityId())
		{
			log.warn("Client with id {} presented device code issued for client {}", loginSession.getEntityId(),
					parsedToken.getClientId());
			// intended - we mask the reason
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
		}

		DeviceCodeStatus status = parsedToken.getDeviceCodeStatus();
		if (status == DeviceCodeStatus.DENIED)
		{
			deviceCodeRepository.remove(deviceCodeValue);
			statisticsPublisher.reportFail(parsedToken.getClientUsername(), parsedToken.getClientName());
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.ACCESS_DENIED, null));
		} else if (status == DeviceCodeStatus.APPROVED)
		{
			return handleApproved(deviceCodeValue, parsedToken, acceptHeader);
		} else
		{
			return handlePending(deviceCodeValue, parsedToken);
		}
	}

	private Response handlePending(String deviceCodeValue, OAuthToken parsedToken) throws OAuthErrorException
	{
		Instant now = Instant.now();
		int interval = parsedToken.getCurrentPollInterval() > 0 ? parsedToken.getCurrentPollInterval()
				: config.getDeviceCodeMinPollInterval();
		Instant lastPolledAt = parsedToken.getLastPolledAt();

		if (lastPolledAt != null && now.isBefore(lastPolledAt.plusSeconds(interval)))
		{
			parsedToken.setCurrentPollInterval(interval + 5);
			parsedToken.setLastPolledAt(now);
			updateRecord(deviceCodeValue, parsedToken);
			throw new OAuthErrorException(BaseOAuthResource.makeError(DeviceAuthorizationGrantError.SLOW_DOWN, null));
		}

		parsedToken.setLastPolledAt(now);
		updateRecord(deviceCodeValue, parsedToken);
		throw new OAuthErrorException(
				BaseOAuthResource.makeError(DeviceAuthorizationGrantError.AUTHORIZATION_PENDING, null));
	}

	private void updateRecord(String deviceCodeValue, OAuthToken token) throws OAuthErrorException
	{
		try
		{
			deviceCodeRepository.update(deviceCodeValue, token, null);
		} catch (JsonProcessingException e)
		{
			log.error("Can not update the device code record", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, "internal error"));
		}
	}

	private Response handleApproved(String deviceCodeValue, OAuthToken parsedToken, String acceptHeader)
			throws OAuthErrorException
	{
		OAuthToken internalToken = new OAuthToken(parsedToken);
		Date now = new Date();
		AccessToken accessToken = accessTokenFactory.create(internalToken, now, acceptHeader);
		internalToken.setAccessToken(accessToken.getValue());

		Long ownerId = parsedToken.getSubjectEntityId();
		RefreshToken refreshToken;
		try
		{
			refreshToken = refreshTokenRepository.createRefreshToken(config, now, internalToken, ownerId)
					.orElse(null);
		} catch (EngineException | JsonProcessingException e)
		{
			log.error("Can not create a refresh token for the device code flow", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, "internal error"));
		}

		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);
		AccessTokenResponse oauthResponse = tokenService.getAccessTokenResponse(internalToken, accessToken,
				refreshToken, null);
		log.info("Device code grant: issuing new access token {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), accessExpiration);

		try
		{
			accessTokenDAO.storeAccessToken(accessToken, internalToken, new EntityParam(ownerId), now,
					accessExpiration);
		} catch (EngineException | JsonProcessingException e)
		{
			log.error("Can not store the access token issued for the device code flow", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, "internal error"));
		}

		deviceCodeRepository.remove(deviceCodeValue);

		statisticsPublisher.reportSuccess(internalToken.getClientUsername(), internalToken.getClientName(),
				new EntityParam(ownerId));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}
}
