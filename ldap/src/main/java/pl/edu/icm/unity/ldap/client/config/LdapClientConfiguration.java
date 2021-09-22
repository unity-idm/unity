/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.config;

import java.util.regex.Pattern;

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.ldap.client.LdapUnsafeArgsEscaper;
import pl.edu.icm.unity.ldap.client.LdapUtils;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.ConnectionMode;

/**
 * Manages configuration of the LDAP client.
 * @author P.Piernik
 *
 */
public class LdapClientConfiguration extends LdapConfiguration
{
	private X509CertChainValidator connectionValidator;
	private Pattern userExtractPattern = null;
	private SearchSpecification searchForUserSpec;
	private Filter parsedValidUserFilter = null;

	public LdapClientConfiguration(LdapProperties prop, PKIManagement pkiMan) throws ConfigurationException
	{
		fromProperties(prop);
		validateConfiguration(pkiMan);

		if (getUsernameExtractorRegexp() != null)
		{
			this.userExtractPattern = Pattern.compile(getUsernameExtractorRegexp());
		}

		if (getConnectionMode() != ConnectionMode.plain)
		{
			if (isTrustAllCerts())

			{
				connectionValidator = new BinaryCertChainValidator(true);
			} else
			{
				try
				{
					connectionValidator = pkiMan.getValidator(getClientTrustStore());
				} catch (EngineException e)
				{
					throw new ConfigurationException(
							"Can't load certificate validator " + "for the ldap client", e);
				}
			}
		}
		if (LdapUtils.nonEmpty(getUserDNSearchKey()))
		{
			try
			{
				searchForUserSpec = new SearchSpecification(getLdapSearchFilter(),
						getLdapSearchBaseName(), null, getLdapSearchScope());
			} catch (LDAPException e)
			{
				throw new ConfigurationException("Invalid search user spec", e);
			}
		}

		try
		{
			parsedValidUserFilter = getValidUserFilter() == null ? Filter.create("objectclass=*")
					: Filter.create(getValidUserFilter());
		} catch (LDAPException e)
		{
			throw new ConfigurationException("Valid users filter is invalid.", e);
		}

	}

	public String getBindDN(String userName)
	{
		String sanitized = LdapUnsafeArgsEscaper.escapeForUseAsDN(userName);
		return getUserDNTemplate().replace(USERNAME_TOKEN, sanitized);
	}

	public X509CertChainValidator getConnectionValidator()
	{
		return connectionValidator;
	}

	public Pattern getUserExtractPattern()
	{
		return userExtractPattern;
	}

	public SearchSpecification getSearchForUserSpec()
	{

		return searchForUserSpec;
	}

	public DereferencePolicy getDereferencePolicy()
	{
		return DereferencePolicy.ALWAYS;
	}

	public boolean isFollowReferral()
	{
		return getFollowReferrals() == 0;
	}

	public long getSocketReadTimeout()
	{
		return getSocketTimeout();
	}

	public int[] getPorts()
	{
		return getServers().stream().mapToInt(s -> s.getPort()).toArray();
	}

	public String[] getServersAddresses()
	{
		return getServers().stream().map(s -> s.getServer()).toArray(String[]::new);
	}

	public SearchScope getSearchScope()
	{
		return SearchScope.SUB;
	}

	public Filter getParsedValidUserFilter()
	{
		return parsedValidUserFilter;
	}
}
