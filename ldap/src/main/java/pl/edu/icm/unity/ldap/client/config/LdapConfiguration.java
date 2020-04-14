/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.config;

import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;
import static pl.edu.icm.unity.ldap.client.config.LdapProperties.PORTS;
import static pl.edu.icm.unity.ldap.client.config.LdapProperties.SERVERS;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.BindAs;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.ConnectionMode;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.SearchScope;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.PasswordRetrievalProperties;
import pl.edu.icm.unity.webui.authn.extensions.TLSRetrievalProperties;

/**
 * Ldap configuration. Used by {@link AuthenticatorEditor} binder.
 * 
 * @author P.Piernik
 *
 */
public class LdapConfiguration
{
	public enum UserDNResolving
	{
		template, ldapSearch
	}
	
	public static final String USERNAME_TOKEN = "{USERNAME}";
	public static final String USER_DN_SEARCH_KEY = "searchUserDN";

	private boolean bindOnly;
	private BindAs bindAs;
	private TranslationProfile translationProfile;
	private String systemDN;
	private String systemPassword;
	private String validUserFilter;
	private UserDNResolving userDNResolving;
	private String userDNTemplate;

	private List<ServerSpecification> servers;
	private ConnectionMode connectionMode;
	private int followReferrals;
	private int searchTimeLimit;
	private int socketTimeout;
	private boolean trustAllCerts;
	private String clientTrustStore;
	private int resultEntriesLimit;

	private boolean delegateGroupFiltering;
	private String groupsBaseName;
	private String memberOfAttribute;
	private String memberOfGroupAttribute;
	private List<GroupSpecification> groupSpecifications;

	private List<String> retrievalLdapAttributes;
	private List<SearchSpecification> searchSpecifications;
	private String usernameExtractorRegexp;

	// Ldap search option fields
	private String userDNSearchKey;
	private String ldapSearchBaseName;
	private String ldapSearchFilter;
	private SearchScope ldapSearchScope;

	private I18nString retrievalName;
	private boolean accountAssociation;
	private String registrationForm;
	
	public LdapConfiguration()
	{
		setBindOnly(LdapProperties.DEFAULT_BIND_ONLY);
		setBindAs(LdapProperties.DEFAULT_BIND_AS);
		setUserDNResolving(UserDNResolving.template);
		setValidUserFilter("objectclass=*");
		setTranslationProfile(TranslationProfileGenerator
				.generateIncludeInputProfile(LdapProperties.DEFAULT_TRANSLATION_PROFILE));
		servers = new ArrayList<>();
		searchSpecifications = new ArrayList<>();
		groupSpecifications = new ArrayList<>();
		setConnectionMode(LdapProperties.DEFAULT_CONNECTION_MODE);
		setFollowReferrals(LdapProperties.DEFAULT_FOLLOW_REFERRALS);
		setSearchTimeLimit(LdapProperties.DEFAULT_SEARCH_TIME_LIMIT);
		setSocketTimeout(LdapProperties.DEFAULT_SOCKET_TIMEOUT);
		setLdapSearchScope(SearchScope.base);
		setDelegateGroupFiltering(LdapProperties.DEFAULT_GROUPS_SEARCH_IN_LDAP);
		setResultEntriesLimit(LdapProperties.DEFAULT_RESULT_ENTRIES_LIMIT);
	}

	public void fromProperties(LdapProperties ldapProp)
	{
		if (ldapProp.isSet(LdapProperties.BIND_ONLY))
		{
			setBindOnly(ldapProp.getBooleanValue(LdapProperties.BIND_ONLY));
		}

		if (ldapProp.isSet(LdapProperties.BIND_AS))
		{
			setBindAs(ldapProp.getEnumValue(LdapProperties.BIND_AS, BindAs.class));
		}

		setSystemDN(ldapProp.getValue(LdapProperties.SYSTEM_DN));
		setSystemPassword(ldapProp.getValue(LdapProperties.SYSTEM_PASSWORD));

		if (ldapProp.isSet(LdapProperties.VALID_USERS_FILTER))
		{
			setValidUserFilter(ldapProp.getValue(LdapProperties.VALID_USERS_FILTER));
		}

		setUserDNTemplate(ldapProp.getValue(LdapProperties.USER_DN_TEMPLATE));
		List<String> server = ldapProp.getListOfValues(SERVERS);
		List<String> ports = ldapProp.getListOfValues(PORTS);

		for (int i = 0; i < Math.max(server.size(), ports.size()); i++)
		{
			int port = -1;
			try
			{
				port = ports.size() > i ? Integer.parseInt(ports.get(i)) : -1;
			} catch (NumberFormatException e)
			{
				//ok
			}
			servers.add(new ServerSpecification(server.size() > i ? server.get(i) : "", port));

		}

		if (ldapProp.isSet(LdapProperties.CONNECTION_MODE))
		{
			setConnectionMode(ldapProp.getEnumValue(LdapProperties.CONNECTION_MODE, ConnectionMode.class));
		}
		if (ldapProp.isSet(LdapProperties.FOLLOW_REFERRALS))
		{
			setFollowReferrals(ldapProp.getIntValue(LdapProperties.FOLLOW_REFERRALS));
		}

		if (ldapProp.isSet(LdapProperties.SEARCH_TIME_LIMIT))
		{
			setSearchTimeLimit(ldapProp.getIntValue(LdapProperties.SEARCH_TIME_LIMIT));
		}
		if (ldapProp.isSet(LdapProperties.SOCKET_TIMEOUT))
		{
			setSocketTimeout(ldapProp.getIntValue(LdapProperties.SOCKET_TIMEOUT));
		}
		if (ldapProp.isSet(LdapProperties.RESULT_ENTRIES_LIMIT))
		{
			setResultEntriesLimit(ldapProp.getIntValue(LdapProperties.RESULT_ENTRIES_LIMIT));
		}

		setTrustAllCerts(ldapProp.getBooleanValue(LdapProperties.TLS_TRUST_ALL));
		setClientTrustStore(ldapProp.getValue(LdapProperties.TRUSTSTORE));
		if (ldapProp.isSet(LdapProperties.GROUPS_SEARCH_IN_LDAP))
		{
			setDelegateGroupFiltering(ldapProp.getBooleanValue(LdapProperties.GROUPS_SEARCH_IN_LDAP));
		}
		setGroupsBaseName(ldapProp.getValue(LdapProperties.GROUPS_BASE_NAME));
		setMemberOfAttribute(ldapProp.getValue(LdapProperties.MEMBER_OF_ATTRIBUTE));
		setMemberOfGroupAttribute(ldapProp.getValue(LdapProperties.MEMBER_OF_GROUP_ATTRIBUTE));

		Set<String> keys = ldapProp.getStructuredListKeys(LdapProperties.GROUP_DEFINITION_PFX);
		for (String key : keys)
		{
			GroupSpecification gs = new GroupSpecification();
			gs.setGroupNameAttribute(ldapProp.getValue(key + LdapProperties.GROUP_DEFINITION_NAME_ATTR));
			gs.setMatchByMemberAttribute(
					ldapProp.getValue(key + LdapProperties.GROUP_DEFINITION_MATCHBY_MEMBER_ATTR));
			gs.setMemberAttribute(ldapProp.getValue(key + LdapProperties.GROUP_DEFINITION_MEMBER_ATTR));
			gs.setObjectClass(ldapProp.getValue(key + LdapProperties.GROUP_DEFINITION_OC));
			groupSpecifications.add(gs);
		}

		Set<String> skeys = ldapProp.getStructuredListKeys(LdapProperties.ADV_SEARCH_PFX);
		userDNSearchKey = ldapProp.getValue(LdapProperties.USER_DN_SEARCH_KEY);

		for (String key : skeys.stream()
				.filter(s -> userDNSearchKey == null
						|| !s.equals(LdapProperties.ADV_SEARCH_PFX + userDNSearchKey + "."))
				.collect(Collectors.toList()))
		{
			SearchSpecification spec = new SearchSpecification();
			String filter = ldapProp.getValue(key + LdapProperties.ADV_SEARCH_FILTER);
			spec.setFilter(filter);
			spec.setBaseDN(ldapProp.getValue(key + LdapProperties.ADV_SEARCH_BASE));
			String attrs = ldapProp.getValue(key + LdapProperties.ADV_SEARCH_ATTRIBUTES);
			spec.setAttributes(attrs);
			spec.setScope(ldapProp.getEnumValue(key + LdapProperties.ADV_SEARCH_SCOPE, SearchScope.class));
			searchSpecifications.add(spec);
		}

		setRetrievalLdapAttributes(ldapProp.getListOfValues(LdapProperties.ATTRIBUTES));

		setUsernameExtractorRegexp(ldapProp.getValue(LdapProperties.USER_ID_EXTRACTOR_REGEXP));

		if (userDNSearchKey != null)
		{
			setUserDNResolving(UserDNResolving.ldapSearch);
			userDNSearchKey = ldapProp.getValue(LdapProperties.USER_DN_SEARCH_KEY);
			setLdapSearchBaseName(ldapProp.getValue(LdapProperties.ADV_SEARCH_PFX + userDNSearchKey + "."
					+ LdapProperties.ADV_SEARCH_BASE));
			setLdapSearchFilter(ldapProp.getValue(LdapProperties.ADV_SEARCH_PFX + userDNSearchKey + "."
					+ LdapProperties.ADV_SEARCH_FILTER));

			// if null, default is used
			if (ldapProp.getEnumValue(LdapProperties.ADV_SEARCH_PFX + userDNSearchKey + "."
					+ LdapProperties.ADV_SEARCH_SCOPE, SearchScope.class) != null)
			{
				setLdapSearchScope(ldapProp.getEnumValue(LdapProperties.ADV_SEARCH_PFX + userDNSearchKey
						+ "." + LdapProperties.ADV_SEARCH_SCOPE, SearchScope.class));
			}

		} else
		{
			setUserDNResolving(UserDNResolving.template);
			setUserDNTemplate(ldapProp.getValue(LdapProperties.USER_DN_TEMPLATE));
		}

		if (ldapProp.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			setTranslationProfile(TranslationProfileGenerator.getProfileFromString(
					ldapProp.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

		} else
		{
			setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
					ldapProp.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE)));
		}

	}

	public void fromProperties(String properties, String type, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the ldap verificator", e);
		}

		LdapProperties ldapProp = new LdapProperties(raw);
		fromProperties(ldapProp);
		
		if (type.equals(LdapPasswordVerificator.NAME))
		{
			fromPasswordRetrievalProperties(raw, msg);
		}else
		{
			fromTLSRetrievalProperties(raw, msg);
		}	
	}

	private void fromPasswordRetrievalProperties(Properties raw, MessageSource msg)
	{
		PasswordRetrievalProperties passwordRetrievalProperties = new PasswordRetrievalProperties(raw);
		setRetrievalName(passwordRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				PasswordRetrievalProperties.NAME));
		setAccountAssociation(passwordRetrievalProperties
				.getBooleanValue(PasswordRetrievalProperties.ENABLE_ASSOCIATION));
		setRegistrationForm(passwordRetrievalProperties
				.getValue(PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN));
		
	}

	private void fromTLSRetrievalProperties(Properties raw, MessageSource msg)
	{
		TLSRetrievalProperties tlsRetrievalProperties = new TLSRetrievalProperties(raw);
		setRetrievalName(tlsRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				TLSRetrievalProperties.NAME));
		setAccountAssociation(tlsRetrievalProperties
				.getBooleanValue(TLSRetrievalProperties.ENABLE_ASSOCIATION));
		setRegistrationForm(tlsRetrievalProperties
				.getValue(TLSRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN));	
	}

	public String toProperties(String type, MessageSource msg) throws ConfigurationException
	{
		Properties raw = new Properties();

		raw.put(LdapProperties.PREFIX + LdapProperties.BIND_AS, bindAs.toString());
		raw.put(LdapProperties.PREFIX + LdapProperties.BIND_ONLY, String.valueOf(bindOnly));

		if (bindAs.equals(BindAs.system) || userDNResolving.equals(UserDNResolving.ldapSearch))
		{
			if (getSystemDN() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.SYSTEM_DN, getSystemDN());
			}

			if (getSystemPassword() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.SYSTEM_PASSWORD, getSystemPassword());
			}
		}

		if (getValidUserFilter() != null)
		{
			raw.put(LdapProperties.PREFIX + LdapProperties.VALID_USERS_FILTER, getValidUserFilter());
		}

		for (int i = 0; i < servers.size(); i++)
		{
			ServerSpecification servConfig = servers.get(i);
			raw.put(LdapProperties.PREFIX + LdapProperties.SERVERS + (i + 1), servConfig.getServer());
			raw.put(LdapProperties.PREFIX + LdapProperties.PORTS + (i + 1),
					String.valueOf(servConfig.getPort()));
		}

		// Server connection config
		raw.put(LdapProperties.PREFIX + LdapProperties.FOLLOW_REFERRALS, String.valueOf(getFollowReferrals()));

		raw.put(LdapProperties.PREFIX + LdapProperties.SEARCH_TIME_LIMIT, String.valueOf(getSearchTimeLimit()));

		raw.put(LdapProperties.PREFIX + LdapProperties.SOCKET_TIMEOUT, String.valueOf(getSocketTimeout()));

		raw.put(LdapProperties.PREFIX + LdapProperties.TLS_TRUST_ALL, String.valueOf(isTrustAllCerts()));
		raw.put(LdapProperties.PREFIX + LdapProperties.RESULT_ENTRIES_LIMIT,
				String.valueOf(getResultEntriesLimit()));

		if (getClientTrustStore() != null)
		{
			raw.put(LdapProperties.PREFIX + LdapProperties.TRUSTSTORE,
					String.valueOf(getClientTrustStore()));
		}

		// Group retrieval settings
		if (!bindOnly)
		{
			raw.put(LdapProperties.PREFIX + LdapProperties.GROUPS_SEARCH_IN_LDAP,
					String.valueOf(isDelegateGroupFiltering()));

			if (getGroupsBaseName() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.GROUPS_BASE_NAME, getGroupsBaseName());
			}

			if (getMemberOfAttribute() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.MEMBER_OF_ATTRIBUTE,
						getMemberOfAttribute());
			}

			if (getMemberOfGroupAttribute() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.MEMBER_OF_GROUP_ATTRIBUTE,
						getMemberOfGroupAttribute());
			}

			if (groupSpecifications != null)
			{
				groupSpecifications.stream().forEach(group -> {
					String prefix = LdapProperties.PREFIX + LdapProperties.GROUP_DEFINITION_PFX
							+ (groupSpecifications.indexOf(group) + 1) + ".";
					if (group.getGroupNameAttribute() != null)
					{
						raw.put(prefix + LdapProperties.GROUP_DEFINITION_NAME_ATTR,
								group.getGroupNameAttribute());
					}

					raw.put(prefix + LdapProperties.GROUP_DEFINITION_MEMBER_ATTR,
							group.getMemberAttribute());
					raw.put(prefix + LdapProperties.GROUP_DEFINITION_OC, group.getObjectClass());

					if (group.getMatchByMemberAttribute() != null)
					{
						raw.put(prefix + LdapProperties.GROUP_DEFINITION_MATCHBY_MEMBER_ATTR,
								group.getMatchByMemberAttribute());
					}

				});
			}

			// Advanced attr search settings
			if (retrievalLdapAttributes != null)
			{

				retrievalLdapAttributes.stream()
						.forEach(a -> raw.put(LdapProperties.PREFIX + LdapProperties.ATTRIBUTES
								+ (retrievalLdapAttributes.indexOf(a) + 1), a));
			}

			if (searchSpecifications != null)
			{

				searchSpecifications.stream().forEach(search -> {
					String prefix = LdapProperties.PREFIX + LdapProperties.ADV_SEARCH_PFX
							+ (searchSpecifications.indexOf(search) + 1) + ".";
					raw.put(prefix + LdapProperties.ADV_SEARCH_BASE, search.getBaseDN());
					raw.put(prefix + LdapProperties.ADV_SEARCH_FILTER, search.getFilter());
					raw.put(prefix + LdapProperties.ADV_SEARCH_SCOPE, search.getScope().toString());

					if (search.getAttributes() != null)
					{
						raw.put(prefix + LdapProperties.ADV_SEARCH_ATTRIBUTES,
								String.join(" ", search.getAttributes()));
					}
				});
			}

			if (getUsernameExtractorRegexp() != null)
			{
				raw.put(LdapProperties.PREFIX + LdapProperties.USER_ID_EXTRACTOR_REGEXP,
						getUsernameExtractorRegexp());
			}

			//Remote data mapping
			try
			{
				raw.put(LdapProperties.PREFIX + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(
								getTranslationProfile().toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize ldap translation profile to JSON", e);
			}

		}

		if (getUserDNResolving().equals(UserDNResolving.template))
		{
			raw.put(LdapProperties.PREFIX + LdapProperties.USER_DN_TEMPLATE, getUserDNTemplate());
		} else
		{
			raw.put(LdapProperties.PREFIX + LdapProperties.USER_DN_SEARCH_KEY, USER_DN_SEARCH_KEY);
			String prefix = LdapProperties.PREFIX + LdapProperties.ADV_SEARCH_PFX + USER_DN_SEARCH_KEY
					+ ".";
			raw.put(prefix + LdapProperties.ADV_SEARCH_BASE, getLdapSearchBaseName());
			raw.put(prefix + LdapProperties.ADV_SEARCH_FILTER, getLdapSearchFilter());
			raw.put(prefix + LdapProperties.ADV_SEARCH_SCOPE, getLdapSearchScope().toString());
		}
		
		if (type.equals(LdapPasswordVerificator.NAME))
		{
			toPasswordRetrievalProperties(raw, msg);
		}else
		{
			toTLSRetrievalProperties(raw, msg);
		}
		

		LdapProperties ldapProp = new LdapProperties(raw);
		return ldapProp.getAsString();
	}
	
	
	private void toPasswordRetrievalProperties(Properties raw, MessageSource msg)
	{
		if (getRetrievalName() != null && !getRetrievalName().isEmpty())
		{
			getRetrievalName().toProperties(raw,
					PasswordRetrievalProperties.P + PasswordRetrievalProperties.NAME, msg);
		}

		raw.put(PasswordRetrievalProperties.P + PasswordRetrievalProperties.ENABLE_ASSOCIATION,
				String.valueOf(isAccountAssociation()));
		if (getRegistrationForm() != null)
		{
			raw.put(PasswordRetrievalProperties.P
					+ PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN,
					getRegistrationForm());
		}		
	}
	
	private void toTLSRetrievalProperties(Properties raw, MessageSource msg)
	{
		if (getRetrievalName() != null && !getRetrievalName().isEmpty())
		{
			getRetrievalName().toProperties(raw,
					TLSRetrievalProperties.P + TLSRetrievalProperties.NAME, msg);
		}

		raw.put(TLSRetrievalProperties.P + TLSRetrievalProperties.ENABLE_ASSOCIATION,
				String.valueOf(isAccountAssociation()));
		if (getRegistrationForm() != null)
		{
			raw.put(TLSRetrievalProperties.P
					+ TLSRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN,
					getRegistrationForm());
		}		
	}
	
	

	public void validateConfiguration(PKIManagement pkiMan) throws ConfigurationException
	{
		validateServersConfiguration();
		validateDNResolving();
		validateUserDNTemplate();
		validateUserDNSearch();
		validateBindAs();
		validateValidUserFilter();
		validateClientTrustStore(pkiMan);
		validateSearchSpecifications();
	}

	private void validateServersConfiguration() throws ConfigurationException
	{
		for (ServerSpecification conf : servers)
		{
			if (conf.getPort() > 65535 || conf.getPort() < 1)
			{
				throw new ConfigurationException("LDAP server port is out of range: " + conf.getPort());
			}
			if (!nonEmpty(conf.getServer()))
			{
				throw new ConfigurationException("LDAP server name is invalid: " + conf.getServer());
			}
		}
	}

	private void validateUserDNTemplate() throws ConfigurationException
	{
		if (nonEmpty(userDNTemplate) && !userDNTemplate.contains(USERNAME_TOKEN))
		{
			throw new ConfigurationException("DN template doesn't contain the mandatory token "
					+ USERNAME_TOKEN + ": " + userDNTemplate);
		}
	}

	private void validateDNResolving() throws ConfigurationException
	{
		if (nonEmpty(userDNTemplate) && nonEmpty(userDNSearchKey))
		{
			throw new ConfigurationException("One and only one of '" + LdapProperties.USER_DN_SEARCH_KEY
					+ "' and '" + LdapProperties.USER_DN_TEMPLATE + "' must be defined");
		}
	}

	private void validateUserDNSearch() throws ConfigurationException
	{

		if (userDNResolving.equals(UserDNResolving.ldapSearch))
		{
			if (((!nonEmpty(systemDN)) || !nonEmpty(systemPassword)) && bindAs != BindAs.none)
			{
				throw new ConfigurationException("To search for users with '"
						+ LdapProperties.USER_DN_SEARCH_KEY
						+ "' system credentials must be defined or bindAs must be set to 'none'.");
			}

			if (!nonEmpty(ldapSearchBaseName) || !nonEmpty(ldapSearchFilter) || ldapSearchScope == null)
			{

				throw new ConfigurationException("A search with the key " + userDNSearchKey
						+ " used for searching users is not correctly defined");
			}

			try
			{
				SearchSpecification.createFilter(ldapSearchFilter, "test");
			} catch (LDAPException e)
			{
				throw new ConfigurationException("A search filter " + ldapSearchFilter + "is invalid");
			}

		} else
		{
			if (!nonEmpty(userDNTemplate) || !userDNTemplate.contains(USERNAME_TOKEN))
				throw new ConfigurationException("DN template doesn't contain the mandatory token "
						+ USERNAME_TOKEN + ": " + userDNTemplate);
		}
	}

	private void validateBindAs() throws ConfigurationException
	{
		if (bindAs == BindAs.system)
		{
			if (systemDN == null || systemPassword == null)
				throw new ConfigurationException("When binding as system all system DN and password "
						+ "name must be configured.");
		}
	}

	private void validateValidUserFilter() throws ConfigurationException
	{
		if (validUserFilter != null)
		{
			try
			{
				Filter.create(validUserFilter);
			} catch (LDAPException e)
			{
				throw new ConfigurationException("Valid users filter is invalid.", e);
			}
		}
	}

	private void validateClientTrustStore(PKIManagement pkiMan) throws ConfigurationException
	{
		if (connectionMode != ConnectionMode.plain && !trustAllCerts)
		{
			try
			{
				pkiMan.getValidator(clientTrustStore);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Invalid client truststore for the ldap client", e);
			}
		}
	}

	private void validateSearchSpecifications() throws ConfigurationException
	{
		for (SearchSpecification spec : searchSpecifications)
		{
			try
			{
				spec.getFilter("test");
			} catch (LDAPException e)
			{
				throw new ConfigurationException(
						"The additional search query filter is invalid: " + spec.getFilter(),
						e);
			}
		}
	}

	public boolean isBindOnly()
	{
		return bindOnly;
	}

	public void setBindOnly(boolean bindOnly)
	{
		this.bindOnly = bindOnly;
	}

	public BindAs getBindAs()
	{
		return bindAs;
	}

	public void setBindAs(BindAs bindAs)
	{
		this.bindAs = bindAs;
	}

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public String getSystemDN()
	{
		return systemDN;
	}

	public void setSystemDN(String systemDN)
	{
		this.systemDN = systemDN;
	}

	public String getSystemPassword()
	{
		return systemPassword;
	}

	public void setSystemPassword(String systemPassword)
	{
		this.systemPassword = systemPassword;
	}

	public String getValidUserFilter()
	{
		return validUserFilter;
	}

	public void setValidUserFilter(String validUserFilter)
	{
		this.validUserFilter = validUserFilter;
	}

	public String getUserDNTemplate()
	{
		return userDNTemplate;
	}

	public void setUserDNTemplate(String userDNTemplate)
	{
		this.userDNTemplate = userDNTemplate;
	}

	public ConnectionMode getConnectionMode()
	{
		return connectionMode;
	}

	public void setConnectionMode(ConnectionMode connectionMode)
	{
		this.connectionMode = connectionMode;
	}

	public List<ServerSpecification> getServers()
	{
		return servers;
	}

	public void setServers(List<ServerSpecification> servers)
	{
		this.servers = servers;
	}

	public int getFollowReferrals()
	{
		return followReferrals;
	}

	public void setFollowReferrals(int followReferrals)
	{
		this.followReferrals = followReferrals;
	}

	public int getSearchTimeLimit()
	{
		return searchTimeLimit;
	}

	public void setSearchTimeLimit(int searchTimeLimit)
	{
		this.searchTimeLimit = searchTimeLimit;
	}

	public int getSocketTimeout()
	{
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout)
	{
		this.socketTimeout = socketTimeout;
	}

	public boolean isTrustAllCerts()
	{
		return trustAllCerts;
	}

	public void setTrustAllCerts(boolean trustAllCerts)
	{
		this.trustAllCerts = trustAllCerts;
	}

	public String getClientTrustStore()
	{
		return clientTrustStore;
	}

	public void setClientTrustStore(String clientTrustStore)
	{
		this.clientTrustStore = clientTrustStore;
	}

	public boolean isDelegateGroupFiltering()
	{
		return delegateGroupFiltering;
	}

	public void setDelegateGroupFiltering(boolean delegateGroupFiltering)
	{
		this.delegateGroupFiltering = delegateGroupFiltering;
	}

	public String getGroupsBaseName()
	{
		return groupsBaseName;
	}

	public void setGroupsBaseName(String groupBaseName)
	{
		this.groupsBaseName = groupBaseName;
	}

	public String getMemberOfAttribute()
	{
		return memberOfAttribute;
	}

	public void setMemberOfAttribute(String memberOfAttribute)
	{
		this.memberOfAttribute = memberOfAttribute;
	}

	public String getMemberOfGroupAttribute()
	{
		return memberOfGroupAttribute;
	}

	public void setMemberOfGroupAttribute(String memberOfGroupAttribute)
	{
		this.memberOfGroupAttribute = memberOfGroupAttribute;
	}

	public List<GroupSpecification> getGroupSpecifications()
	{
		return groupSpecifications;
	}

	public void setGroupSpecifications(List<GroupSpecification> groups)
	{
		this.groupSpecifications = groups;
	}

	public List<String> getRetrievalLdapAttributes()
	{
		return retrievalLdapAttributes;
	}

	public void setRetrievalLdapAttributes(List<String> retrievalLdapAttributes)
	{
		this.retrievalLdapAttributes = retrievalLdapAttributes;
	}

	public List<SearchSpecification> getSearchSpecifications()
	{
		return searchSpecifications;
	}

	public void setSearchSpecifications(List<SearchSpecification> searchSpecifications)
	{
		this.searchSpecifications = searchSpecifications;
	}

	public String getUsernameExtractorRegexp()
	{
		return usernameExtractorRegexp;
	}

	public void setUsernameExtractorRegexp(String usernameExtractorRegexp)
	{
		this.usernameExtractorRegexp = usernameExtractorRegexp;
	}

	public UserDNResolving getUserDNResolving()
	{
		return userDNResolving;
	}

	public void setUserDNResolving(UserDNResolving userDNResolving)
	{
		this.userDNResolving = userDNResolving;
	}

	public String getLdapSearchBaseName()
	{
		return ldapSearchBaseName;
	}

	public void setLdapSearchBaseName(String ldapSearchBaseName)
	{
		this.ldapSearchBaseName = ldapSearchBaseName;
	}

	public String getLdapSearchFilter()
	{
		return ldapSearchFilter;
	}

	public void setLdapSearchFilter(String ldapSearchFilter)
	{
		this.ldapSearchFilter = ldapSearchFilter;
	}

	public SearchScope getLdapSearchScope()
	{
		return ldapSearchScope;
	}

	public void setLdapSearchScope(SearchScope ldapSearchScope)
	{
		this.ldapSearchScope = ldapSearchScope;
	}

	public int getResultEntriesLimit()
	{
		return resultEntriesLimit;
	}

	public void setResultEntriesLimit(int resultEntriesLimit)
	{
		this.resultEntriesLimit = resultEntriesLimit;
	}

	public String getUserDNSearchKey()
	{
		return userDNSearchKey;
	}

	public I18nString getRetrievalName()
	{
		return retrievalName;
	}

	public void setRetrievalName(I18nString retrievalName)
	{
		this.retrievalName = retrievalName;
	}

	public boolean isAccountAssociation()
	{
		return accountAssociation;
	}

	public void setAccountAssociation(boolean accountAssociation)
	{
		this.accountAssociation = accountAssociation;
	}

	public String getRegistrationForm()
	{
		return registrationForm;
	}

	public void setRegistrationForm(String registrationForm)
	{
		this.registrationForm = registrationForm;
	}
}