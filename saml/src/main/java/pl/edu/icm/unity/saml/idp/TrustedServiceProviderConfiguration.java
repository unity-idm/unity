/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.types.I18nString;

import java.security.cert.X509Certificate;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TrustedServiceProviderConfiguration
{
	public final String allowedKey;
	public final String dnSamlId;
	public final String entityId;
	public final boolean encrypt;
	public final String returnUrl;
	public final Set<String> returnUrls;
	public final String soapLogoutUrl;
	public final String redirectLogoutUrl;
	public final String postLogoutUrl;
	public final String redirectLogoutRetUrl;
	public final String postLogoutRetUrl;
	public final I18nString name;
	public final I18nString logoUri;
	public final String certificateName;
	public final Set<String> certificateNames;
	public final X509Certificate certificate;
	public final Set<X509Certificate> certificates;

	TrustedServiceProviderConfiguration(String allowedKey, String dnSamlId, String entityId, boolean encrypt, String returnUrl,
	                                            Set<String> returnUrls, String soapLogoutUrl, String redirectLogoutUrl,
	                                            String postLogoutUrl, String redirectLogoutRetUrl, String postLogoutRetUrl,
	                                            I18nString name, I18nString logoUri, X509Certificate certificate,
	                                            Set<X509Certificate> certificates, String certificateName, Set<String> certificateNames)
	{
		this.allowedKey = allowedKey;
		this.dnSamlId = dnSamlId;
		this.entityId = entityId;
		this.encrypt = encrypt;
		this.returnUrl = returnUrl;
		this.returnUrls = returnUrls;
		this.soapLogoutUrl = soapLogoutUrl;
		this.redirectLogoutUrl = redirectLogoutUrl;
		this.postLogoutUrl = postLogoutUrl;
		this.redirectLogoutRetUrl = redirectLogoutRetUrl;
		this.postLogoutRetUrl = postLogoutRetUrl;
		this.name = name;
		this.logoUri = logoUri;
		this.certificate = certificate;
		this.certificates = certificates;
		this.certificateName = certificateName;
		this.certificateNames = certificateNames;
	}

	public List<SAMLEndpointDefinition> getLogoutEndpointsFromStructuredList()
	{
		String postSlo = postLogoutUrl;
		String redirectSlo = redirectLogoutUrl;
		String postRetSlo = postLogoutRetUrl;
		String redirectRetSlo = redirectLogoutRetUrl;
		String soapSlo = soapLogoutUrl;

		if (isBlank(redirectRetSlo))
			redirectRetSlo = redirectSlo;
		if (isBlank(postRetSlo))
			postRetSlo = postSlo;

		List<SAMLEndpointDefinition> ret = new ArrayList<>(3);
		if (!isBlank(postSlo))
			ret.add(new SAMLEndpointDefinition(SamlProperties.Binding.HTTP_POST, postSlo, postRetSlo));
		if (!isBlank(redirectSlo))
			ret.add(new SAMLEndpointDefinition(SamlProperties.Binding.HTTP_REDIRECT, redirectSlo, redirectRetSlo));
		if (!isBlank(soapSlo))
			ret.add(new SAMLEndpointDefinition(SamlProperties.Binding.SOAP, soapSlo, soapSlo));
		return ret;
	}

	public Set<X509Certificate> getCertificates()
	{
		Set<X509Certificate> idpCertNames = new HashSet<>();
		if (certificate != null)
			idpCertNames.add(certificate);
		idpCertNames.addAll(certificates);
		return idpCertNames;
	}

	public Set<String> getCertificateNames()
	{
		Set<String> idpCertNames = new HashSet<>();
		if (certificateName != null)
			idpCertNames.add(certificateName);
		idpCertNames.addAll(certificateNames);
		return idpCertNames;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TrustedServiceProviderConfiguration that = (TrustedServiceProviderConfiguration) o;
		return encrypt == that.encrypt && Objects.equals(dnSamlId, that.dnSamlId) &&
				Objects.equals(entityId, that.entityId) &&
				Objects.equals(returnUrl, that.returnUrl) &&
				Objects.equals(returnUrls, that.returnUrls) &&
				Objects.equals(soapLogoutUrl, that.soapLogoutUrl) &&
				Objects.equals(redirectLogoutUrl, that.redirectLogoutUrl) &&
				Objects.equals(postLogoutUrl, that.postLogoutUrl) &&
				Objects.equals(redirectLogoutRetUrl, that.redirectLogoutRetUrl) &&
				Objects.equals(postLogoutRetUrl, that.postLogoutRetUrl) &&
				Objects.equals(name, that.name) &&
				Objects.equals(logoUri, that.logoUri) &&
				Objects.equals(certificateName, that.certificateName) &&
				Objects.equals(certificateNames, that.certificateNames) &&
				Objects.equals(certificate, that.certificate) &&
				Objects.equals(certificates, that.certificates);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dnSamlId, entityId, encrypt, returnUrl, returnUrls, soapLogoutUrl, redirectLogoutUrl,
				postLogoutUrl, redirectLogoutRetUrl, postLogoutRetUrl, name, logoUri, certificate, certificates,
				certificateName, certificateNames);
	}

	@Override
	public String toString()
	{
		return "TrustedServiceProviderConfiguration{" +
				"dnSamlId='" + dnSamlId + '\'' +
				", entityId='" + entityId + '\'' +
				", encrypt=" + encrypt +
				", returnUrl='" + returnUrl + '\'' +
				", returnUrls=" + returnUrls +
				", soapLogoutUrl='" + soapLogoutUrl + '\'' +
				", redirectLogoutUrl='" + redirectLogoutUrl + '\'' +
				", postLogoutUrl='" + postLogoutUrl + '\'' +
				", redirectLogoutRetUrl='" + redirectLogoutRetUrl + '\'' +
				", postLogoutRetUrl='" + postLogoutRetUrl + '\'' +
				", name='" + name + '\'' +
				", logoUri='" + logoUri + '\'' +
				", certificateName='" + certificateName + '\'' +
				", certificatesNames=" + certificateNames +
				'}';
	}

	public static TrustedServiceProviderConfigurationBuilder builder()
	{
		return new TrustedServiceProviderConfigurationBuilder();
	}

	public TrustedServiceProviderConfigurationBuilder copyToBuilder()
	{
		return new TrustedServiceProviderConfigurationBuilder()
				.withAllowedKey(allowedKey)
				.withDnSamlId(dnSamlId)
				.withEntityId(entityId)
				.withEncrypt(encrypt)
				.withReturnUrl(returnUrl)
				.withReturnUrls(returnUrls)
				.withSoapLogoutUrl(soapLogoutUrl)
				.withRedirectLogoutUrl(redirectLogoutUrl)
				.withPostLogoutUrl(postLogoutUrl)
				.withRedirectLogoutRetUrl(redirectLogoutRetUrl)
				.withPostLogoutRetUrl(postLogoutRetUrl)
				.withName(name)
				.withLogoUri(logoUri)
				.withCertificate(certificate)
				.withCertificates(certificates)
				.withCertificateName(certificateName)
				.withCertificateNames(certificateNames);
	}

	public static final class TrustedServiceProviderConfigurationBuilder
	{
		public String allowedKey;
		public String dnSamlId;
		public String entityId;
		public boolean encrypt;
		public String returnUrl;
		public Set<String> returnUrls;
		public String soapLogoutUrl;
		public String redirectLogoutUrl;
		public String postLogoutUrl;
		public String redirectLogoutRetUrl;
		public String postLogoutRetUrl;
		public I18nString name;
		public I18nString logoUri;
		public String certificateName;
		public Set<String> certificateNames;
		public X509Certificate certificate;
		public Set<X509Certificate> certificates;

		private TrustedServiceProviderConfigurationBuilder()
		{
		}

		public TrustedServiceProviderConfigurationBuilder withAllowedKey(String allowedKey)
		{
			this.allowedKey = allowedKey;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withDnSamlId(String dnSamlId)
		{
			this.dnSamlId = dnSamlId;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withEntityId(String entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withEncrypt(boolean encrypt)
		{
			this.encrypt = encrypt;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withReturnUrl(String returnUrl)
		{
			this.returnUrl = returnUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withReturnUrls(Set<String> returnUrls)
		{
			this.returnUrls = returnUrls;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withSoapLogoutUrl(String soapLogoutUrl)
		{
			this.soapLogoutUrl = soapLogoutUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withRedirectLogoutUrl(String redirectLogoutUrl)
		{
			this.redirectLogoutUrl = redirectLogoutUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withPostLogoutUrl(String postLogoutUrl)
		{
			this.postLogoutUrl = postLogoutUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withRedirectLogoutRetUrl(String redirectLogoutRetUrl)
		{
			this.redirectLogoutRetUrl = redirectLogoutRetUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withPostLogoutRetUrl(String postLogoutRetUrl)
		{
			this.postLogoutRetUrl = postLogoutRetUrl;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withName(I18nString name)
		{
			this.name = name;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withLogoUri(I18nString logoUri)
		{
			this.logoUri = logoUri;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withCertificate(X509Certificate certificate)
		{
			this.certificate = certificate;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withCertificates(Set<X509Certificate> certificates)
		{
			this.certificates = certificates;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withCertificateName(String certificateName)
		{
			this.certificateName = certificateName;
			return this;
		}

		public TrustedServiceProviderConfigurationBuilder withCertificateNames(Set<String> certificateNames)
		{
			this.certificateNames = certificateNames;
			return this;
		}

		public TrustedServiceProviderConfiguration build()
		{
			return new TrustedServiceProviderConfiguration(allowedKey, dnSamlId, entityId, encrypt, returnUrl, returnUrls,
					soapLogoutUrl, redirectLogoutUrl, postLogoutUrl, redirectLogoutRetUrl, postLogoutRetUrl, name, logoUri,
					certificate, certificates, certificateName, certificateNames);
		}
	}
}