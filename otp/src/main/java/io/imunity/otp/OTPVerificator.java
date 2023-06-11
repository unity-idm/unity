/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.imunity.otp.credential_reset.OTPCredentialDBState;
import io.imunity.otp.credential_reset.OTPCredentialReset;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.util.Date;

@PrototypeComponent
public class OTPVerificator extends AbstractLocalVerificator implements OTPExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OTP, OTPVerificator.class);
	public static final String DESC = "One-time password";
	public static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};

	private OTPCredentialDefinition credentialConfig;
	private final CredentialHelper credentialHelper;
	private final NotificationProducer notificationProducer;
	
	@Autowired
	OTPVerificator(CredentialHelper credentialHelper, NotificationProducer notificationProducer)
	{
		super(OTP.NAME, DESC, OTPExchange.ID, true);
		this.credentialHelper = credentialHelper;
		this.notificationProducer = notificationProducer;
	}
	
	@Override
	public String getExchangeId()
	{
		return OTPExchange.ID;
	}

	@Override
	public AuthenticationResult verifyCode(String codeFromUser, AuthenticationSubject subject)
	{
		return checkCode(subject, codeFromUser);
	}

	private AuthenticationResult checkCode(AuthenticationSubject subject, String code)
	{
		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveSubject(subject, IDENTITY_TYPES, credentialName);
		} catch (Exception e)
		{
			log.info("The user for OTP authN can not be found: " + subject, e);
			return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"), e);
		}
		
		try
		{
			String dbCredential = resolved.getCredentialValue();
			OTPCredentialDBState credState = JsonUtil.parse(dbCredential, OTPCredentialDBState.class);
			
			boolean valid = TOTPCodeVerificator.verifyCode(code, credState.secret, credState.otpParams,
					credentialConfig.allowedTimeDriftSteps);
			
			if (!valid)
			{
				log.info("Code provided by {} is invalid", subject);
				return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"));
			}
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), subject, 
					credState.outdated ? resolved.getCredentialName() : null);
			return LocalAuthenticationResult.successful(ae);
		} catch (Exception e)
		{
			log.warn("Error during TOTP verification for " + subject, e);
			return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"), e);
		}
	}
	
	
	@Override
	public OTPCredentialReset getCredentialResetBackend()
	{
		return new OTPCredentialReset(notificationProducer, 
				identityResolver, 
				this, 
				credentialHelper, 
				credentialName, 
				(ObjectNode) JsonUtil.toJsonNode(credentialConfig), 
				credentialConfig.resetSettings);
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verifyNew)
			throws IllegalCredentialException, InternalException
	{
		OTPCredential credential = JsonUtil.parse(rawCredential, OTPCredential.class);
		OTPCredentialDBState dbState = new OTPCredentialDBState(credential.secret, credential.otpParams,
				new Date(), false, null);
		return JsonUtil.toJsonString(dbState);
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException
	{
		if (Strings.isNullOrEmpty(currentCredential))
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		OTPCredentialDBState dbState = JsonUtil.parse(currentCredential, OTPCredentialDBState.class);
		OTPExtraInfo extraInfo = new OTPExtraInfo(dbState.time);
		String extraInfoStr = JsonUtil.toJsonString(extraInfo);
		return new CredentialPublicInformation(dbState.outdated ? LocalCredentialState.outdated : LocalCredentialState.correct, 
				extraInfoStr);
	}

	@Override
	public String invalidate(String currentCredential)
	{
		OTPCredentialDBState dbState = JsonUtil.parse(currentCredential, OTPCredentialDBState.class);
		OTPCredentialDBState invalidated = new OTPCredentialDBState(dbState.secret, dbState.otpParams,
				dbState.time, true, null);
		return JsonUtil.toJsonString(invalidated);
	}

	@Override
	public boolean isCredentialSet(EntityParam entity) throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		OTPCredentialDefinition newDefinition = JsonUtil.parse(newCredentialDefinition, OTPCredentialDefinition.class);
		if (!newDefinition.otpParams.equals(credentialConfig.otpParams))
			return true;
		//changing issuer name and drift should be OK
		return false;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return JsonUtil.toJsonString(credentialConfig);
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		credentialConfig = JsonUtil.parse(config, OTPCredentialDefinition.class);
	}

	@Override
	public int getCodeLength()
	{
		return credentialConfig.otpParams.codeLength;
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<OTPVerificator> factory)
		{
			super(OTP.NAME, DESC, true, factory);
		}
	}
}
