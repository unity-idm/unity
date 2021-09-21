/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.unboundid.ldap.sdk.LDAPException;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.OTPCredentialReset;
import io.imunity.otp.OTPExchange;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;
import io.imunity.otp.TOTPCodeVerificator;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.config.LdapClientConfiguration;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Identity;

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
			ldapClientConfiguration = new LdapClientConfiguration(otpWithLDAPProperties.toFullLDAPProperties(), pkiManagement);
	
		} catch(ConfigurationException e)
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
		return new NoOTPCredentialResetImpl();
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
		
		OTPGenerationParams otpParams = new OTPGenerationParams(otpLdapConfiguration.getCodeLength(),
				otpLdapConfiguration.getHashFunction(), otpLdapConfiguration.getTimeStepSeconds());
		try
		{	
			String secret = getSecretFromLdap(resolved.getValue());
			boolean valid = TOTPCodeVerificator.verifyCode(code, secret, otpParams, 
					otpLdapConfiguration.getAllowedTimeDriftSteps());
			
			if (!valid)
			{
				log.info("Code provided by {} is invalid", subject);
				return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"));
			}
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), subject, 
					 null);
			return LocalAuthenticationResult.successful(ae);
		} catch (Exception e)
		{
			log.warn("Error during TOTP verification for " + subject, e);
			return LocalAuthenticationResult.failed(new ResolvableError("OTPRetrieval.wrongCode"), e);
		}
	}
	
	private String getSecretFromLdap(String usernameIdentity) throws AuthenticationException, KeyManagementException,
			LDAPException, NoSuchAlgorithmException, LdapAuthenticationException
	{
		Optional<String> secret = ldapClient.searchAttribute(usernameIdentity,
				otpLdapConfiguration.getSecretAttribute(), ldapClientConfiguration);
		if (!secret.isPresent())
		{
			throw new AuthenticationException("OTP secret is not available for user " + usernameIdentity);
		}
		return secret.get();
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
	
	private static class NoOTPCredentialResetImpl extends OTPCredentialReset
	{
		public NoOTPCredentialResetImpl()
		{
			super(null, null, null, null, null,
					null, new OTPResetSettings(false, 0, null, null, null) );
		}	
	}
}
