/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Image;

import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;

/**
 * Read-side business logic for {@link DeviceSignInView}: resolving a user-entered code into a
 * pending device-code record, and preparing the data a consent screen needs to show. Nothing here
 * mutates persisted state - see {@link DeviceCodeTransitionService} for that.
 */
@Component
class DeviceSignInWorkflowPreparer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceSignInWorkflowPreparer.class);

	private final MessageSource msg;
	private final OAuthEndpointsCoordinator coordinator;
	private final DeviceCodeRepository deviceCodeRepository;
	private final DeviceCodeVerificationThrottle throttle;
	private final OAuthIdPEngine idpEngine;
	private final OAuthRequestValidatorFactory requestValidatorFactory;
	private final AttributeTypeSupport aTypeSupport;
	private final PolicyAgreementManagement policyAgreementsMan;

	@Autowired
	DeviceSignInWorkflowPreparer(MessageSource msg, OAuthEndpointsCoordinator coordinator,
			DeviceCodeRepository deviceCodeRepository, DeviceCodeVerificationThrottle throttle, IdPEngine idPEngine,
			OAuthRequestValidatorFactory requestValidatorFactory, AttributeTypeSupport aTypeSupport,
			PolicyAgreementManagement policyAgreementsMan)
	{
		this.msg = msg;
		this.coordinator = coordinator;
		this.deviceCodeRepository = deviceCodeRepository;
		this.throttle = throttle;
		this.idpEngine = new OAuthIdPEngine(idPEngine);
		this.requestValidatorFactory = requestValidatorFactory;
		this.aTypeSupport = aTypeSupport;
		this.policyAgreementsMan = policyAgreementsMan;
	}

	enum ResolveErrorReason
	{
		INVALID_CODE, TOO_MANY_ATTEMPTS, EXPIRED_CODE, ALREADY_PROCESSED, DISABLED
	}

	sealed interface ResolveResult permits Resolved, ResolveError
	{
	}

	record Resolved(String deviceCodeValue, DeviceCodeToken parsedToken, OAuthASProperties config)
			implements ResolveResult
	{
	}

	record ResolveError(ResolveErrorReason reason) implements ResolveResult
	{
	}

	/**
	 * Looks up a device sign-in {@code user_code}, applying the same checks (and side effects on
	 * {@link DeviceCodeVerificationThrottle}) as before this class existed: rate limiting, expiry,
	 * PENDING status, and the target client's device-grant being enabled.
	 */
	ResolveResult resolve(String userCode, long entityId)
	{
		if (userCode == null || userCode.isBlank())
			return new ResolveError(ResolveErrorReason.INVALID_CODE);

		if (throttle.getRemainingBlockedTimeMs(entityId) > 0)
			return new ResolveError(ResolveErrorReason.TOO_MANY_ATTEMPTS);

		Optional<Token> tokenOpt = deviceCodeRepository.findByUserCode(userCode);
		if (tokenOpt.isEmpty())
		{
			throttle.unsuccessfulAttempt(entityId);
			return new ResolveError(ResolveErrorReason.INVALID_CODE);
		}
		Token token = tokenOpt.get();
		DeviceCodeToken parsed = DeviceCodeToken.getInstanceFromJson(token.getContents());

		if (token.getExpires() != null && token.getExpires().before(new Date()))
		{
			throttle.unsuccessfulAttempt(entityId);
			return new ResolveError(ResolveErrorReason.EXPIRED_CODE);
		}
		if (parsed.getDeviceCodeStatus() != DeviceCodeStatus.PENDING)
			// not a guessing signal - most likely the user's own code, already acted upon
			return new ResolveError(ResolveErrorReason.ALREADY_PROCESSED);

		Optional<OAuthASProperties> configOpt = coordinator.getDeviceSignInConfig(parsed.getOauthToken().getIssuerUri());
		if (configOpt.isEmpty() || !configOpt.get().isDeviceGrantEnabled())
			return new ResolveError(ResolveErrorReason.DISABLED);

		throttle.successfulAttempt(entityId);
		return new Resolved(token.getValue(), parsed, configOpt.get());
	}

	List<PolicyAgreementConfiguration> filterAgreementsToPresent(OAuthASProperties config, long entityId)
	{
		List<PolicyAgreementConfiguration> toPresent = new ArrayList<>();
		try
		{
			toPresent.addAll(policyAgreementsMan.filterAgreementToPresent(new EntityParam(entityId),
					CommonIdPProperties.getPolicyAgreementsConfig(msg, config).agreements));
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept", e);
		}
		return toPresent;
	}

	record ConsentPresentation(IdentityParam identity, String clientName, Image clientLogo,
			Set<DynamicAttribute> attributes, Optional<ActiveValueSelectionConfig> activeValueSelectionConfig)
	{
	}

	/**
	 * Resolves the identity and attributes to show on the consent screen for the given (already
	 * resolved and PENDING) device code, mirroring {@code OAuthProcessor}'s authorization_code
	 * consent preparation.
	 */
	ConsentPresentation prepareConsent(DeviceCodeToken parsedToken, OAuthASProperties config, long entityId)
			throws Exception
	{
		OAuthToken oauthToken = parsedToken.getOauthToken();
		OAuthRequestValidator requestValidator = requestValidatorFactory.getOAuthRequestValidator(config);
		Map<String, AttributeExt> clientAttributes = requestValidator
				.getAttributesNoAuthZ(new EntityParam(oauthToken.getClientId()));

		String usersGroup = getUsersGroup(clientAttributes, config);
		EntityInGroup requesterEntity = new EntityInGroup(config.getValue(OAuthASProperties.CLIENTS_GROUP),
				new EntityParam(oauthToken.getClientId()));

		TranslationResult translationResult = idpEngine.getUserInfoUnsafe(entityId, oauthToken.getClientUsername(),
				Optional.of(requesterEntity), usersGroup, config.getOutputTranslationProfile(),
				GrantFlow.deviceCode.toString(), config, null);

		IdentityParam identity = idpEngine.getIdentity(translationResult, config.getSubjectIdentityType());

		Set<String> requestedAttributes = new HashSet<>();
		for (RequestedOAuthScope si : oauthToken.getEffectiveScope())
			requestedAttributes.addAll(si.scopeDefinition().attributes());
		Set<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(translationResult, requestedAttributes);

		Image clientLogo = buildClientLogo(clientAttributes);
		String clientName = oauthToken.getClientName() != null ? oauthToken.getClientName()
				: oauthToken.getClientUsername();

		Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = ActiveValueClientHelper
				.getActiveValueSelectionConfig(config.getActiveValueClients(), oauthToken.getClientUsername(),
						attributes);

		return new ConsentPresentation(identity, clientName, clientLogo, attributes, activeValueSelectionConfig);
	}

	private String getUsersGroup(Map<String, AttributeExt> clientAttributes, OAuthASProperties config)
	{
		AttributeExt groupA = clientAttributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP);
		return groupA != null ? (String) groupA.getValues().get(0) : config.getValue(OAuthASProperties.USERS_GROUP);
	}

	private Image buildClientLogo(Map<String, AttributeExt> clientAttributes)
	{
		Attribute logoAttr = clientAttributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
		if (logoAttr == null || !ImageAttributeSyntax.ID.equals(logoAttr.getValueSyntax()))
			return null;
		ImageAttributeSyntax syntax = (ImageAttributeSyntax) aTypeSupport.getSyntax(logoAttr);
		UnityImage image = syntax.convertFromString(logoAttr.getValues().get(0).toString());
		String filename = "%s.%s".formatted(UUID.randomUUID(), image.getType().toExt());
		return new Image(DownloadHandlers.forUnityImage(image, filename), "");
	}
}
