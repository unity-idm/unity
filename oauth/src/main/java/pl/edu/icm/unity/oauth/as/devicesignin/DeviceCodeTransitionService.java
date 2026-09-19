/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.oauth.as.AttributeFilteringSpec;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Owns the transactional read-modify-write side of a device sign-in decision: applying an
 * approve/reject transition to the pending {@link DeviceCodeToken} record, and (on approve) minting
 * the OIDC ID token. See {@link DeviceSignInWorkflowPreparer} for the read-only side.
 */
@Component
class DeviceCodeTransitionService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceCodeTransitionService.class);

	private final TransactionalRunner tx;
	private final DeviceCodeRepository deviceCodeRepository;

	enum TransitionResult
	{
		APPLIED, ALREADY_PROCESSED, FAILED
	}

	@Autowired
	DeviceCodeTransitionService(TransactionalRunner tx, DeviceCodeRepository deviceCodeRepository)
	{
		this.tx = tx;
		this.deviceCodeRepository = deviceCodeRepository;
	}

	/**
	 * Approves the device code: stamps the subject, user info, validity and (if requested) an OIDC ID
	 * token onto its {@code OAuthToken}, then marks it APPROVED so the device's {@code /token} poll
	 * can pick it up.
	 */
	TransitionResult approve(String deviceCodeValue, IdentityParam identity, UserInfo userInfo,
			Collection<DynamicAttribute> activeValueSelectionFilteredAttributes, long entityId,
			Instant authenticationTime, OAuthASProperties config)
	{
		List<AttributeFilteringSpec> activeValueFilters = activeValueSelectionFilteredAttributes == null ? null
				: mapSelectedAttributesToFilters(activeValueSelectionFilteredAttributes);

		return applyTransitionAtomically(deviceCodeValue, currentToken ->
		{
			OAuthToken oauthToken = currentToken.getOauthToken();
			oauthToken.setSubject(identity.getValue());
			oauthToken.setUserInfo(userInfo.toJSONObject().toJSONString());
			oauthToken.setAuthenticationTime(authenticationTime);
			oauthToken.setTokenValidity(config.getAccessTokenValidity());
			oauthToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
			oauthToken.setAudience(List.of(oauthToken.getClientUsername()));
			if (activeValueFilters != null)
				oauthToken.setAttributeValueFilters(activeValueFilters);

			if (!signAndRecordIdTokenIfRequested(oauthToken, userInfo, config))
				return false;

			currentToken.setSubjectEntityId(entityId);
			currentToken.setDeviceCodeStatus(DeviceCodeStatus.APPROVED);
			return true;
		});
	}

	/**
	 * Denies/cancels the device code, marking it DENIED so the device's {@code /token} poll observes
	 * the rejection.
	 */
	TransitionResult reject(String deviceCodeValue)
	{
		return applyTransitionAtomically(deviceCodeValue, currentToken ->
		{
			currentToken.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
			return true;
		});
	}

	private List<AttributeFilteringSpec> mapSelectedAttributesToFilters(Collection<DynamicAttribute> attributes)
	{
		return attributes.stream()
				.map(a -> new AttributeFilteringSpec(a.getAttribute().getName(),
						a.getAttribute().getValues().stream().collect(Collectors.toSet())))
				.toList();
	}

	/**
	 * Mirrors {@code OAuthProcessor}'s id token generation for the authorization_code flow: an OIDC
	 * ID token is minted once, at consent time, and carried in {@code openidInfo} for later reuse
	 * (device_code grant handling and any subsequent refresh both just decode it, they never sign a
	 * fresh one). Without this, clients requesting the {@code openid} scope would silently get an
	 * OAuth-only response, even though discovery advertises OIDC support for the device grant.
	 */
	private boolean signAndRecordIdTokenIfRequested(OAuthToken oauthToken, UserInfo userInfo, OAuthASProperties config)
	{
		boolean openIdRequested = oauthToken.getEffectiveScope()
				.stream()
				.anyMatch(s -> OAuthSystemScopeProvider.OPENID_SCOPE.equals(s.scope()));
		if (!openIdRequested)
			return true;

		try
		{
			Date now = new Date();
			IDTokenClaimsSet idToken = new IDTokenClaimsSet(new Issuer(config.getIssuerName()),
					new Subject(oauthToken.getSubject()),
					oauthToken.getAudience().stream().filter(a -> a != null).map(Audience::new).toList(),
					new Date(now.getTime() + config.getIdTokenValidity() * 1000L), now);
			idToken.setAuthenticationTime(Date.from(oauthToken.getAuthenticationTime()));
			if (oauthToken.hasSupportAttributesInIdToken().orElse(false))
				idToken.putAll(userInfo);

			JWT idTokenSigned = config.getTokenSigner().sign(idToken);
			oauthToken.setOpenidToken(idTokenSigned.serialize());
			return true;
		} catch (Exception e)
		{
			log.error("Cannot create the OpenID Connect ID token for the device code grant", e);
			return false;
		}
	}

	/**
	 * Atomically re-reads the pending record under a database row lock, applies {@code mutator} only
	 * if it is still PENDING, and writes it back - all within one transaction. This closes two
	 * races: a concurrent /token poll transitioning or removing the same record, and a second
	 * browser session (duplicate tab, or resubmission) trying to accept/deny/cancel a record that
	 * was already decided.
	 */
	private TransitionResult applyTransitionAtomically(String deviceCodeValue, Predicate<DeviceCodeToken> mutator)
	{
		try
		{
			return tx.runInTransactionRet(() ->
			{
				Optional<Token> current = deviceCodeRepository.getByDeviceCodeForUpdate(deviceCodeValue);
				if (current.isEmpty())
					return TransitionResult.ALREADY_PROCESSED;
				DeviceCodeToken currentToken = DeviceCodeToken.getInstanceFromJson(current.get().getContents());
				if (currentToken.getDeviceCodeStatus() != DeviceCodeStatus.PENDING)
					return TransitionResult.ALREADY_PROCESSED;
				if (!mutator.test(currentToken))
					return TransitionResult.FAILED;
				try
				{
					deviceCodeRepository.update(deviceCodeValue, currentToken, null);
				} catch (JsonProcessingException e)
				{
					throw new RuntimeException(e);
				}
				return TransitionResult.APPLIED;
			});
		} catch (Exception e)
		{
			log.error("Can not update the device code record", e);
			return TransitionResult.FAILED;
		}
	}
}
