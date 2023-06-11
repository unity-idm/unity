/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.config;

import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.unboundid.ldap.sdk.LDAPException;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.BindAs;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonConfiguration;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.SearchScope;
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
public class LdapConfiguration extends LDAPCommonConfiguration
{

	private boolean bindOnly;
	private BindAs bindAs;
	private TranslationProfile translationProfile;


	private boolean delegateGroupFiltering;
	private String groupsBaseName;
	private String memberOfAttribute;
	private String memberOfGroupAttribute;
	private List<GroupSpecification> groupSpecifications;

	private List<String> retrievalLdapAttributes;
	private List<SearchSpecification> searchSpecifications;

	private String userDNSearchKey;
	
	private I18nString retrievalName;
	private boolean accountAssociation;
	private String registrationForm;
	
	public LdapConfiguration()
	{
		super();
		setBindOnly(LdapProperties.DEFAULT_BIND_ONLY);
		setBindAs(LdapProperties.DEFAULT_BIND_AS);
		setTranslationProfile(TranslationProfileGenerator
				.generateIncludeInputProfile(LdapProperties.DEFAULT_TRANSLATION_PROFILE));
		searchSpecifications = new ArrayList<>();
		groupSpecifications = new ArrayList<>();
		setDelegateGroupFiltering(LdapProperties.DEFAULT_GROUPS_SEARCH_IN_LDAP);
		setResultEntriesLimit(LdapProperties.DEFAULT_RESULT_ENTRIES_LIMIT);
	}

	public void fromProperties(LdapProperties ldapProp)
	{
		super.fromProperties(ldapProp);
		
		if (ldapProp.isSet(LdapProperties.BIND_ONLY))
		{
			setBindOnly(ldapProp.getBooleanValue(LdapProperties.BIND_ONLY));
		}

		if (ldapProp.isSet(LdapProperties.BIND_AS))
		{
			setBindAs(ldapProp.getEnumValue(LdapProperties.BIND_AS, BindAs.class));
		}
		
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
		super.toProperties(LdapProperties.PREFIX, raw, msg);

		raw.put(LdapProperties.PREFIX + LdapProperties.BIND_AS, bindAs.toString());
		raw.put(LdapProperties.PREFIX + LdapProperties.BIND_ONLY, String.valueOf(bindOnly));

		if (bindAs.equals(BindAs.system) || getUserDNResolving().equals(UserDNResolving.ldapSearch))
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
		super.validateConfiguration(pkiMan);
		validateDNResolving();
		validateUserDNSearch();
		validateBindAs();
		validateSearchSpecifications();
	}

	private void validateDNResolving() throws ConfigurationException
	{
		if (nonEmpty(getUserDNTemplate()) && nonEmpty(userDNSearchKey))
		{
			throw new ConfigurationException("One and only one of '" + LdapProperties.USER_DN_SEARCH_KEY
					+ "' and '" + LdapProperties.USER_DN_TEMPLATE + "' must be defined");
		}
	}

	private void validateUserDNSearch() throws ConfigurationException
	{

		if (getUserDNResolving().equals(UserDNResolving.ldapSearch))
		{
			if (((!nonEmpty(getSystemDN())) || !nonEmpty(getSystemPassword())) && bindAs != BindAs.none)
			{
				throw new ConfigurationException("To search for users with '"
						+ LdapProperties.USER_DN_SEARCH_KEY
						+ "' system credentials must be defined or bindAs must be set to 'none'.");
			}

			if (!nonEmpty(getLdapSearchBaseName()) || !nonEmpty(getLdapSearchFilter()) || getLdapSearchScope() == null)
			{

				throw new ConfigurationException("A search with the key " + userDNSearchKey
						+ " used for searching users is not correctly defined");
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
			if (!nonEmpty(getUserDNTemplate()) || !getUserDNTemplate().contains(USERNAME_TOKEN))
				throw new ConfigurationException("DN template doesn't contain the mandatory token "
						+ USERNAME_TOKEN + ": " + getUserDNTemplate());
		}
	}

	private void validateBindAs() throws ConfigurationException
	{
		if (bindAs == BindAs.system)
		{
			if (getSystemDN() == null || getSystemPassword() == null)
				throw new ConfigurationException("When binding as system all system DN and password "
						+ "name must be configured.");
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