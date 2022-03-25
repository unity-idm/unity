/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.TranslationProfile;

public class TrustedIdPConfiguration
{
	public final String samlId;
	public final String idpEndpointURL;
	public final Set<String> certificateNames;
	public final String groupMembershipAttribute;
	public final boolean signRequest;
	public final String requestedNameFormat;
	public final List<PublicKey> publicKeys;
	public final TrustedIdPKey key;
	public final I18nString name;
	public final I18nString logoURI;
	public final Set<String> tags;
	public final List<SAMLEndpointDefinition> logoutEndpoints;
	public final String federationId;
	public final String federationName;
	public final String registrationForm;
	public final boolean enableAccountsAssocation;
	public final Binding binding;
	public final TranslationProfile translationProfile;

	private TrustedIdPConfiguration(Builder builder)
	{
		checkNotNull(builder.certificateNames);
		checkNotNull(builder.name);
		checkNotNull(builder.publicKeys);
		checkNotNull(builder.key);
		checkNotNull(builder.samlId);
		checkNotNull(builder.tags);
		checkNotNull(builder.binding);
		checkNotNull(builder.logoutEndpoints);
		
		this.samlId = builder.samlId;
		this.idpEndpointURL = builder.idpEndpointURL;
		this.certificateNames = Set.copyOf(builder.certificateNames);
		this.groupMembershipAttribute = builder.groupMembershipAttribute;
		this.signRequest = builder.signRequest;
		this.requestedNameFormat = builder.requestedNameFormat;
		this.publicKeys = List.copyOf(builder.publicKeys);
		this.key = builder.key;
		this.name = builder.name;
		this.logoURI = builder.logoURI;
		this.tags = Set.copyOf(builder.tags);
		this.logoutEndpoints = List.copyOf(builder.logoutEndpoints);
		this.federationId = builder.federationId;
		this.federationName = builder.federationName;
		this.registrationForm = builder.registrationForm;
		this.enableAccountsAssocation = builder.enableAccountsAssocation;
		this.binding = builder.binding;
		this.translationProfile = builder.translationProfile;
	}	

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(binding, certificateNames, enableAccountsAssocation,
				federationId, federationName, groupMembershipAttribute, idpEndpointURL, key, logoURI,
				logoutEndpoints, name, publicKeys, registrationForm, requestedNameFormat, samlId,
				signRequest, tags, translationProfile);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrustedIdPConfiguration other = (TrustedIdPConfiguration) obj;
		return binding == other.binding && Objects.equals(certificateNames, other.certificateNames)
				&& enableAccountsAssocation == other.enableAccountsAssocation
				&& Objects.equals(federationId, other.federationId)
				&& Objects.equals(federationName, other.federationName)
				&& Objects.equals(groupMembershipAttribute, other.groupMembershipAttribute)
				&& Objects.equals(idpEndpointURL, other.idpEndpointURL)
				&& Objects.equals(key, other.key) && Objects.equals(logoURI, other.logoURI)
				&& Objects.equals(logoutEndpoints, other.logoutEndpoints)
				&& Objects.equals(name, other.name) && Objects.equals(publicKeys, other.publicKeys)
				&& Objects.equals(registrationForm, other.registrationForm)
				&& Objects.equals(requestedNameFormat, other.requestedNameFormat)
				&& Objects.equals(samlId, other.samlId) && signRequest == other.signRequest
				&& Objects.equals(tags, other.tags)
				&& Objects.equals(translationProfile, other.translationProfile);
	}

	public static final class Builder
	{
		private String samlId;
		private String idpEndpointURL;
		private Set<String> certificateNames = Collections.emptySet();
		private String groupMembershipAttribute;
		private boolean signRequest;
		private String requestedNameFormat;
		private List<PublicKey> publicKeys = Collections.emptyList();
		private TrustedIdPKey key;
		private I18nString name;
		private I18nString logoURI;
		private Set<String> tags = Collections.emptySet();
		private List<SAMLEndpointDefinition> logoutEndpoints = new ArrayList<>();
		private String federationId;
		private String federationName;
		private String registrationForm;
		private boolean enableAccountsAssocation;
		private Binding binding;
		private TranslationProfile translationProfile;

		private Builder()
		{
		}

		public Builder withSamlId(String samlId)
		{
			this.samlId = samlId;
			return this;
		}

		public Builder withIdpEndpointURL(String idpEndpointURL)
		{
			this.idpEndpointURL = idpEndpointURL;
			return this;
		}

		public Builder withCertificateNames(Set<String> certificateNames)
		{
			this.certificateNames = certificateNames;
			return this;
		}

		public Builder withGroupMembershipAttribute(String groupMembershipAttribute)
		{
			this.groupMembershipAttribute = groupMembershipAttribute;
			return this;
		}

		public Builder withSignRequest(boolean signRequest)
		{
			this.signRequest = signRequest;
			return this;
		}

		public Builder withRequestedNameFormat(String requestedNameFormat)
		{
			this.requestedNameFormat = requestedNameFormat;
			return this;
		}

		public Builder withPublicKeys(List<PublicKey> publicKeys)
		{
			this.publicKeys = publicKeys;
			return this;
		}

		public Builder withKey(TrustedIdPKey key)
		{
			this.key = key;
			return this;
		}

		public Builder withName(I18nString name)
		{
			this.name = name;
			return this;
		}

		public Builder withLogoURI(I18nString logoURI)
		{
			this.logoURI = logoURI;
			return this;
		}

		public Builder withTags(Set<String> tags)
		{
			this.tags = tags;
			return this;
		}

		public Builder withLogoutEndpoint(SAMLEndpointDefinition logoutEndpoint)
		{
			this.logoutEndpoints.add(logoutEndpoint);
			return this;
		}

		public Builder withLogoutEndpoints(Collection<SAMLEndpointDefinition> logoutEndpoints)
		{
			this.logoutEndpoints.addAll(logoutEndpoints);
			return this;
		}

		public Builder withFederationId(String federationId)
		{
			this.federationId = federationId;
			return this;
		}

		public Builder withFederationName(String federationName)
		{
			this.federationName = federationName;
			return this;
		}

		public Builder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		public Builder withEnableAccountsAssocation(boolean enableAccountsAssocation)
		{
			this.enableAccountsAssocation = enableAccountsAssocation;
			return this;
		}

		public Builder withBinding(Binding binding)
		{
			this.binding = binding;
			return this;
		}

		public Builder withTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public TrustedIdPConfiguration build()
		{
			return new TrustedIdPConfiguration(this);
		}
	}
	
	
	
}