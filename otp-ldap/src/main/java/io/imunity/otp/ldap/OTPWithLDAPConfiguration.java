/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPRetrievalProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.ldap.client.config.LdapConfiguration;
import pl.edu.icm.unity.ldap.client.config.SearchSpecification;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonConfiguration;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.SearchScope;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;

/**
 * OTP-LDAP configuration. Used by {@link AuthenticatorEditor} binder.
 * 
 * @author P.Piernik
 *
 */
public class OTPWithLDAPConfiguration extends LDAPCommonConfiguration
{
	private String secretAttribute;

	private int codeLength;
	private HashFunction hashFunction;
	private int timeStepSeconds;
	private int allowedTimeDriftSteps = 3;

	private I18nString retrievalName;

	public OTPWithLDAPConfiguration()
	{
		super();
		setCodeLength(OTPWithLDAPProperties.DEFAULT_OTP_CODE_LENGHT);
		setHashFunction(OTPWithLDAPProperties.DEFAULT_OTP_HASH_FUNCTION);
		setAllowedTimeDriftSteps(OTPWithLDAPProperties.DEFAULT_OTP_ALLOWED_TIME_DRIFT_STEPS);
		setTimeStepSeconds(OTPWithLDAPProperties.DEFAULT_OTP_TIME_STEP_SECODS);
	}

	public void fromProperties(OTPWithLDAPProperties otpWithLDAPProperties)
	{
		super.fromProperties(otpWithLDAPProperties);
		
		
		if (otpWithLDAPProperties.getValue(OTPWithLDAPProperties.USER_DN_TEMPLATE) == null)
		{
			setUserDNResolving(UserDNResolving.ldapSearch);
			setLdapSearchBaseName(otpWithLDAPProperties.getValue(OTPWithLDAPProperties.LDAP_SEARCH_BASENAME));
			setLdapSearchFilter(otpWithLDAPProperties.getValue(OTPWithLDAPProperties.LDAP_SEARCH_FILTER));

			// if null, default is used
			if (otpWithLDAPProperties.getEnumValue(OTPWithLDAPProperties.LDAP_SEARCH_SCOPE, SearchScope.class) != null)
			{
				setLdapSearchScope(
						otpWithLDAPProperties.getEnumValue(OTPWithLDAPProperties.LDAP_SEARCH_SCOPE, SearchScope.class));
			}

		} else
		{
			setUserDNResolving(UserDNResolving.template);
			setUserDNTemplate(otpWithLDAPProperties.getValue(OTPWithLDAPProperties.USER_DN_TEMPLATE));
		}

		setCodeLength(otpWithLDAPProperties.getIntValue(OTPWithLDAPProperties.OTP_CODE_LENGHT));
		setTimeStepSeconds(otpWithLDAPProperties.getIntValue(OTPWithLDAPProperties.OTP_TIME_STEP_SECODS));
		setHashFunction(
				otpWithLDAPProperties.getEnumValue(OTPWithLDAPProperties.OTP_HASH_FUNCTION, HashFunction.class));
		setSecretAttribute(otpWithLDAPProperties.getValue(OTPWithLDAPProperties.OTP_SECRET_ATTRIBUTE));
		setAllowedTimeDriftSteps(otpWithLDAPProperties.getIntValue(OTPWithLDAPProperties.OTP_ALLOWED_TIME_DRIFT_STEPS));
	}

	public void fromProperties(String properties, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the ldap verificator", e);
		}

		OTPWithLDAPProperties otpWithLDAPProperties = new OTPWithLDAPProperties(raw);
		fromProperties(otpWithLDAPProperties);

		OTPRetrievalProperties retrievalProperties = new OTPRetrievalProperties(raw);
		setRetrievalName(retrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				OTPRetrievalProperties.NAME));
		
	}

	public String toProperties(MessageSource msg) throws ConfigurationException
	{
		Properties raw = new Properties();
		super.toProperties(OTPWithLDAPProperties.PREFIX, raw, msg);
		
		if (getSystemDN() != null)
		{
			raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.SYSTEM_DN, getSystemDN());
		}

		if (getSystemPassword() != null)
		{
			raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.SYSTEM_PASSWORD, getSystemPassword());
		}
		
		if (getUserDNResolving().equals(UserDNResolving.template))
		{
			raw.put(OTPWithLDAPProperties.PREFIX + LDAPCommonProperties.USER_DN_TEMPLATE, getUserDNTemplate());
		} else
		{			
			raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.LDAP_SEARCH_BASENAME, getLdapSearchBaseName());
			raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.LDAP_SEARCH_FILTER, getLdapSearchFilter());
			raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.LDAP_SEARCH_SCOPE, getLdapSearchScope().toString());
		}

		raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.OTP_CODE_LENGHT, String.valueOf(codeLength));
		raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.OTP_ALLOWED_TIME_DRIFT_STEPS,
				String.valueOf(allowedTimeDriftSteps));
		raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.OTP_HASH_FUNCTION, hashFunction.toString());
		raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.OTP_SECRET_ATTRIBUTE, secretAttribute);
		raw.put(OTPWithLDAPProperties.PREFIX + OTPWithLDAPProperties.OTP_TIME_STEP_SECODS,
				String.valueOf(timeStepSeconds));

		if (getRetrievalName() != null && !getRetrievalName().isEmpty())
		{
			getRetrievalName().toProperties(raw,
					OTPRetrievalProperties.P + OTPRetrievalProperties.NAME, msg);
		}
		
		
		OTPWithLDAPProperties otpLdapProp = new OTPWithLDAPProperties(raw);
		return otpLdapProp.getAsString();
	}

	public void validateConfiguration(PKIManagement pkiMan) throws ConfigurationException
	{
		super.validateConfiguration(pkiMan);
		validateDNResolving();
		validateUserDNTemplate();
		validateUserDNSearch();
		validateBindAs();
		validateValidUserFilter();
	}

	private void validateUserDNTemplate() throws ConfigurationException
	{
		String userDNTemplate = getUserDNTemplate();
		
		if (nonEmpty(userDNTemplate) && !userDNTemplate.contains(LdapConfiguration.USERNAME_TOKEN))
		{
			throw new ConfigurationException(
					"DN template doesn't contain the mandatory token " + LdapConfiguration.USERNAME_TOKEN + ": " + userDNTemplate);
		}
	}

	private void validateDNResolving() throws ConfigurationException
	{
		if (nonEmpty(getUserDNTemplate()) && nonEmpty(getLdapSearchBaseName()))
		{
			throw new ConfigurationException("One and only one of '" + OTPWithLDAPProperties.LDAP_SEARCH_BASENAME + "' and '"
					+ LDAPCommonProperties.USER_DN_TEMPLATE + "' must be defined");
		}
	}

	private void validateUserDNSearch() throws ConfigurationException
	{

		if (getUserDNResolving().equals(UserDNResolving.ldapSearch))
		{
			if (!nonEmpty(getLdapSearchBaseName()) || !nonEmpty(getLdapSearchFilter()) || getLdapSearchScope() == null)
			{

				throw new ConfigurationException("A search used for searching users is not correctly defined");
			}

			try
			{
				SearchSpecification.createFilter(getLdapSearchFilter(), "test");
			} catch (LDAPException e)
			{
				throw new ConfigurationException("A search filter " + getLdapSearchFilter() + "is invalid");
			}

		} else
		{
			if (!nonEmpty(getUserDNTemplate()) || !getUserDNTemplate().contains(LdapConfiguration.USERNAME_TOKEN))
				throw new ConfigurationException(
						"DN template doesn't contain the mandatory token " + LdapConfiguration.USERNAME_TOKEN + ": " + getUserDNTemplate());
		}
	}

	private void validateBindAs() throws ConfigurationException
	{

		if (getSystemDN() == null || getSystemPassword() == null)
			throw new ConfigurationException(
					"When binding as system all system DN and password " + "name must be configured.");

	}

	private void validateValidUserFilter() throws ConfigurationException
	{
		if (getValidUserFilter() != null)
		{
			try
			{
				Filter.create(getValidUserFilter());
			} catch (LDAPException e)
			{
				throw new ConfigurationException("Valid users filter is invalid.", e);
			}
		}
	}

	public I18nString getRetrievalName()
	{
		return retrievalName;
	}

	public void setRetrievalName(I18nString retrievalName)
	{
		this.retrievalName = retrievalName;
	}

	public String getSecretAttribute()
	{
		return secretAttribute;
	}

	public void setSecretAttribute(String secretAttribute)
	{
		this.secretAttribute = secretAttribute;
	}

	public int getCodeLength()
	{
		return codeLength;
	}

	public void setCodeLength(int codeLength)
	{
		this.codeLength = codeLength;
	}

	public HashFunction getHashFunction()
	{
		return hashFunction;
	}

	public void setHashFunction(HashFunction hashFunction)
	{
		this.hashFunction = hashFunction;
	}

	public int getTimeStepSeconds()
	{
		return timeStepSeconds;
	}

	public void setTimeStepSeconds(int timeStepSeconds)
	{
		this.timeStepSeconds = timeStepSeconds;
	}

	public int getAllowedTimeDriftSteps()
	{
		return allowedTimeDriftSteps;
	}

	public void setAllowedTimeDriftSteps(int allowedTimeDriftSteps)
	{
		this.allowedTimeDriftSteps = allowedTimeDriftSteps;
	}

}