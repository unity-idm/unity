/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.TOTPKeyGenerator;
import io.imunity.otp.credential_reset.OTPCredentialReset;
import io.imunity.otp.OTPExchange;
import io.imunity.otp.TOTPCodeVerificator;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.config.LdapClientConfiguration;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@PrototypeComponent
class OTPWithLDAPVerificator extends AbstractVerificator implements OTPExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OTP, OTPWithLDAPVerificator.class);

	public static final String NAME = "otp-ldap";
	public static final String DESC = "One-time password with ldap";

	private final PKIManagement pkiManagement;

	private OTPWithLDAPProperties otpWithLDAPProperties;
	private LdapClientConfiguration ldapClientConfiguration;
	private OTPWithLDAPConfiguration otpLdapConfiguration;
	private LdapClient ldapClient;

	OTPWithLDAPVerificator(PKIManagement pkiManagement)
	{
		super(NAME, DESC, OTPExchange.ID);
		this.pkiManagement = pkiManagement;
		this.ldapClient = new LdapClient();
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Mixed;
	}

	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			otpWithLDAPProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OTP-LDAP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			otpWithLDAPProperties = new OTPWithLDAPProperties(properties);
			otpLdapConfiguration = new OTPWithLDAPConfiguration();
			otpLdapConfiguration.fromProperties(otpWithLDAPProperties);
			ldapClientConfiguration = new LdapClientConfiguration(otpWithLDAPProperties.toFullLDAPProperties(),
					pkiManagement);

		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OTP-LDAP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OTP-LDAP verificator(?)", e);
		}
	}

	@Override
	public int getCodeLength()
	{
		return otpLdapConfiguration.getCodeLength();
	}

	@Override
	public OTPCredentialReset getCredentialResetBackend()
	{
		return OTPCredentialReset.createDisabled();
	}

	@Override
	public AuthenticationResult verifyCode(String code, AuthenticationSubject subject)
	{
		Identity resolved;
		try
		{
			resolved = identityResolver.resolveSubject(subject, UsernameIdentity.ID);
		} catch (Exception e)
		{
			log.info("The user for OTP authN can not be found: " + subject, e);
			return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"), e);
		}
		
		try
		{
			OTPURIParams otpURIParams = getOTPURIParamsFromLdapFallbackToDefaults(resolved.getValue());
			boolean valid = TOTPCodeVerificator.verifyCode(
					code, otpURIParams.base32Secret, new OTPGenerationParams(otpURIParams.codeLength,
							otpURIParams.hashFunction, otpURIParams.timeStepSeconds),
					otpLdapConfiguration.getAllowedTimeDriftSteps());

			if (!valid)
			{
				log.info("Code provided by {} is invalid", subject);
				return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"));
			}
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), subject, null);
			return LocalAuthenticationResult.successful(ae, getAuthenticationMethod());
		} catch (Exception e)
		{
			log.warn("Error during TOTP verification for " + subject, e);
			return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"), e);
		}
	}

	private OTPURIParams getOTPURIParamsFromLdapFallbackToDefaults(String usernameIdentity) throws AuthenticationException,
			KeyManagementException, LDAPException, NoSuchAlgorithmException, LdapAuthenticationException
	{
		Optional<String> secretURIOpt = ldapClient.searchAttribute(usernameIdentity,
				otpLdapConfiguration.getSecretAttribute(), ldapClientConfiguration);

		if (!secretURIOpt.isPresent())
		{
			log.error("OTP secret URI is not available for user " + usernameIdentity);
			throw new AuthenticationException("OTP secret is not available for user " + usernameIdentity);
		}
		Map<String, String> queryParams = null;
		try
		{
			queryParams = URIBuilderFixer.newInstance(secretURIOpt.get()).getQueryParams().stream()
					.collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
		} catch (URISyntaxException e)
		{
			log.error("Can not parse secret URI from LDAP", e);
			throw new AuthenticationException("OTP secret is not available for user " + usernameIdentity);
		}

		String base32Secret = queryParams.get(TOTPKeyGenerator.SECRET_URI_PARAM);
		if (base32Secret == null || base32Secret.isEmpty())
		{
			log.error("OTP secret is not available for user " + usernameIdentity);
			throw new AuthenticationException("OTP secret is not available for user " + usernameIdentity);
		}

		HashFunction hashFunction = HashFunction.valueOf(queryParams.getOrDefault(TOTPKeyGenerator.ALGORITHM_URI_PARAM,
				otpLdapConfiguration.getHashFunction().toString()));
		int codeLenght = Integer.valueOf(queryParams.getOrDefault(TOTPKeyGenerator.DIGITS_URI_PARAM,
				String.valueOf(otpLdapConfiguration.getCodeLength())));
		int timeStepSeconds = Integer.valueOf(queryParams.getOrDefault(TOTPKeyGenerator.PERIOD_URI_PARAM,
				String.valueOf(otpLdapConfiguration.getTimeStepSeconds())));

		return new OTPURIParams(hashFunction, timeStepSeconds, codeLenght, base32Secret);

	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.OTP;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<OTPWithLDAPVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
	
	private static class OTPURIParams
	{
		final HashFunction hashFunction;
		final int timeStepSeconds;
		final int codeLength;
		final String base32Secret;
		
		OTPURIParams(HashFunction hashFunction, int timeStepSeconds, int codeLength,
				String base32Secret)
		{
		
			this.hashFunction = hashFunction;
			this.timeStepSeconds = timeStepSeconds;
			this.codeLength = codeLength;
			this.base32Secret = base32Secret;
		}
	}
}
