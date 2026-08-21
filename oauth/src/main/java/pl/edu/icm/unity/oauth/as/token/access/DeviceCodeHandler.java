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
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationGrantError;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
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
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
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

	/**
	 * RFC 6749 §5.2 / RFC 8628 §3.5: token endpoint error responses use HTTP 400 for every error
	 * except invalid_client (401). {@link OAuth2Error#ACCESS_DENIED} bakes in HTTP 403, which is
	 * correct for the authorization endpoint's redirect-based error (RFC 6749 §4.1.2.1) but wrong
	 * here - some device-client libraries reject a 403 outright before ever parsing the OAuth error
	 * body, so the client would never learn it was actually an access_denied.
	 */
	private static final ErrorObject ACCESS_DENIED_TOKEN_ERROR = new ErrorObject(OAuth2Error.ACCESS_DENIED_CODE,
			OAuth2Error.ACCESS_DENIED.getDescription(), HTTPResponse.SC_BAD_REQUEST);

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

	Response handleDeviceCodeGrant(String deviceCode, String clientId, String acceptHeader) throws EngineException
	{
		try
		{
			return tx.runInTransactionRetThrowing(() -> handleInTransaction(deviceCode, clientId, acceptHeader));
		} catch (OAuthErrorException e)
		{
			return e.response;
		}
	}

	private Response handleInTransaction(String deviceCodeValue, String clientId, String acceptHeader)
			throws OAuthErrorException
	{
		if (!config.isDeviceGrantEnabled())
		{
			statisticsPublisher.reportFailAsLoggedClient();
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "device grant disabled"));
		}

		// locked for the rest of this transaction: a concurrent poll or browser approval/denial on
		// the same device code must be serialized against this one, otherwise both could observe
		// APPROVED and issue tokens, or a concurrent write here and in DeviceSignInView could clobber
		// each other
		Optional<Token> tokenOpt = deviceCodeRepository.getByDeviceCodeForUpdate(deviceCodeValue);
		if (tokenOpt.isEmpty())
		{
			statisticsPublisher.reportFailAsLoggedClient();
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
		}

		Token deviceToken = tokenOpt.get();
		DeviceCodeToken parsedToken = DeviceCodeToken.getInstanceFromJson(deviceToken.getContents());
		OAuthToken oauthToken = parsedToken.getOauthToken();

		// the device_code namespace is shared by all deployed OAuth AS endpoints; a code must only
		// ever be redeemable through the endpoint that issued it, so that a different endpoint's
		// disabled grant, client group or signing/refresh policy can never apply to it
		String currentIssuer = config.getValue(OAuthASProperties.ISSUER_URI);
		if (!currentIssuer.equals(oauthToken.getIssuerUri()))
		{
			statisticsPublisher.reportFailAsLoggedClient();
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
		}

		if (deviceToken.getExpires() != null && deviceToken.getExpires().before(new Date()))
		{
			deviceCodeRepository.remove(deviceCodeValue, parsedToken.getUserCode());
			statisticsPublisher.reportFail(oauthToken.getClientUsername(), oauthToken.getClientName());
			return BaseOAuthResource.makeError(DeviceAuthorizationGrantError.EXPIRED_TOKEN, null);
		}

		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		assertClientAuthenticated(oauthToken, loginSession, clientId);

		DeviceCodeStatus status = parsedToken.getDeviceCodeStatus();
		if (status == DeviceCodeStatus.DENIED)
		{
			deviceCodeRepository.remove(deviceCodeValue, parsedToken.getUserCode());
			statisticsPublisher.reportFail(oauthToken.getClientUsername(), oauthToken.getClientName());
			return BaseOAuthResource.makeError(ACCESS_DENIED_TOKEN_ERROR, null);
		} else if (status == DeviceCodeStatus.APPROVED)
		{
			return handleApproved(deviceCodeValue, parsedToken, acceptHeader);
		} else
		{
			return handlePending(deviceCodeValue, parsedToken);
		}
	}

	/**
	 * RFC 8628 §3.4: confidential clients must authenticate as for any other token request; public
	 * clients (who cannot authenticate) must identify themselves with client_id, as required by
	 * RFC 6749 §4.1.3 for unauthenticated token requests.
	 */
	private void assertClientAuthenticated(OAuthToken oauthToken, LoginSession loginSession, String clientId)
			throws OAuthErrorException
	{
		if (loginSession != null)
		{
			if (oauthToken.getClientId() != loginSession.getEntityId())
			{
				log.warn("Client with id {} presented device code issued for client {}", loginSession.getEntityId(),
						oauthToken.getClientId());
				statisticsPublisher.reportFail(oauthToken.getClientUsername(), oauthToken.getClientName());
				// intended - we mask the reason
				throw new OAuthErrorException(
						BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
			}
			return;
		}

		if (oauthToken.getClientType() == ClientType.CONFIDENTIAL)
		{
			statisticsPublisher.reportFail(oauthToken.getClientUsername(), oauthToken.getClientName());
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_CLIENT, "not authenticated"));
		}

		if (clientId == null || !clientId.equals(oauthToken.getClientUsername()))
		{
			log.warn("Client {} presented device code issued for client {}", clientId,
					oauthToken.getClientUsername());
			statisticsPublisher.reportFail(oauthToken.getClientUsername(), oauthToken.getClientName());
			// intended - we mask the reason
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong device_code"));
		}
	}

	private Response handlePending(String deviceCodeValue, DeviceCodeToken parsedToken) throws OAuthErrorException
	{
		Instant now = Instant.now();
		int interval = parsedToken.getCurrentPollInterval() > 0 ? parsedToken.getCurrentPollInterval()
				: config.getDeviceCodeMinPollInterval();
		Instant lastPolledAt = parsedToken.getLastPolledAt();

		if (lastPolledAt != null && now.isBefore(lastPolledAt.plusSeconds(interval)))
		{
			// rejected, read-only: lastPolledAt is left untouched, so this device code keeps being
			// rejected at no storage cost - however fast a client (ab)polls - until real time
			// actually advances past lastPolledAt + interval. Writing bookkeeping on every rejected
			// poll would let a client turn a tight polling loop into a DB write per request.
			return BaseOAuthResource.makeError(DeviceAuthorizationGrantError.SLOW_DOWN, null);
		}

		parsedToken.setLastPolledAt(now);
		updateRecord(deviceCodeValue, parsedToken);
		return BaseOAuthResource.makeError(DeviceAuthorizationGrantError.AUTHORIZATION_PENDING, null);
	}

	private void updateRecord(String deviceCodeValue, DeviceCodeToken token) throws OAuthErrorException
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

	private Response handleApproved(String deviceCodeValue, DeviceCodeToken parsedToken, String acceptHeader)
			throws OAuthErrorException
	{
		OAuthToken internalToken = new OAuthToken(parsedToken.getOauthToken());
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

		deviceCodeRepository.remove(deviceCodeValue, parsedToken.getUserCode());

		statisticsPublisher.reportSuccess(internalToken.getClientUsername(), internalToken.getClientName(),
				new EntityParam(ownerId));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}
}
