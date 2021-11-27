/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.Properties;

import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;

/**
 * SAML service trusted federation configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLServiceTrustedFederationConfiguration
{
	private String name;
	private String url;
	private String httpsTruststore;
	private boolean ignoreSignatureVerification;
	private String signatureVerificationCertificate;
	private int refreshInterval;

	public SAMLServiceTrustedFederationConfiguration()
	{
		setRefreshInterval(SamlProperties.DEFAULT_METADATA_REFRESH);
		setIgnoreSignatureVerification(true);
	}

	public void fromProperties(SamlIdpProperties source, String name)
	{
		setName(name);
		String prefix = SamlIdpProperties.SPMETA_PREFIX + name + ".";

		setUrl(source.getValue(prefix + SamlProperties.METADATA_URL));
		setHttpsTruststore(source.getValue(prefix + SamlProperties.METADATA_HTTPS_TRUSTSTORE));

		if (source.isSet(prefix + SamlProperties.METADATA_SIGNATURE))
		{
			setIgnoreSignatureVerification(source
					.getEnumValue(prefix + SamlProperties.METADATA_SIGNATURE,
							MetadataSignatureValidation.class)
					.equals(MetadataSignatureValidation.ignore));
		}

		setSignatureVerificationCertificate(source.getValue(prefix + SamlProperties.METADATA_ISSUER_CERT));
		if (source.isSet(prefix + SamlProperties.METADATA_REFRESH))
		{
			setRefreshInterval(source.getIntValue(prefix + SamlProperties.METADATA_REFRESH));
		}
	}

	public void toProperties(Properties raw)
	{
		String prefix = SamlIdpProperties.P + SamlIdpProperties.SPMETA_PREFIX + getName() + ".";

		raw.put(prefix + SamlProperties.METADATA_URL, getUrl());

		if (getHttpsTruststore() != null)
		{
			raw.put(prefix + SamlProperties.METADATA_HTTPS_TRUSTSTORE, getHttpsTruststore());
		}

		if (!isIgnoreSignatureVerification())
		{
			raw.put(prefix + SamlProperties.METADATA_SIGNATURE,
					MetadataSignatureValidation.require.toString());
		} else
		{
			raw.put(prefix + SamlProperties.METADATA_SIGNATURE,
					MetadataSignatureValidation.ignore.toString());
		}

		if (getSignatureVerificationCertificate() != null)
		{
			raw.put(prefix + SamlProperties.METADATA_ISSUER_CERT, getSignatureVerificationCertificate());
		}

		raw.put(prefix + SamlProperties.METADATA_REFRESH, String.valueOf(getRefreshInterval()));
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getHttpsTruststore()
	{
		return httpsTruststore;
	}

	public void setHttpsTruststore(String httpsTruststore)
	{
		this.httpsTruststore = httpsTruststore;
	}

	public boolean isIgnoreSignatureVerification()
	{
		return ignoreSignatureVerification;
	}

	public void setIgnoreSignatureVerification(boolean ignoreSignatureVerification)
	{
		this.ignoreSignatureVerification = ignoreSignatureVerification;
	}

	public String getSignatureVerificationCertificate()
	{
		return signatureVerificationCertificate;
	}

	public void setSignatureVerificationCertificate(String signatureVerificationCertificate)
	{
		this.signatureVerificationCertificate = signatureVerificationCertificate;
	}

	public int getRefreshInterval()
	{
		return refreshInterval;
	}

	public void setRefreshInterval(int refreshInterval)
	{
		this.refreshInterval = refreshInterval;
	}

	public SAMLServiceTrustedFederationConfiguration clone()
	{
		SAMLServiceTrustedFederationConfiguration clone = new SAMLServiceTrustedFederationConfiguration();

		clone.setName(new String(this.getName()));

		clone.setUrl(this.getUrl() != null ? new String(this.getUrl()) : null);
		clone.setHttpsTruststore(
				this.getHttpsTruststore() != null ? new String(this.getHttpsTruststore()) : null);

		clone.setIgnoreSignatureVerification(this.ignoreSignatureVerification);

		clone.setSignatureVerificationCertificate(this.getSignatureVerificationCertificate() != null
				? new String(this.getSignatureVerificationCertificate())
				: null);

		clone.setRefreshInterval(this.getRefreshInterval());

		return clone;
	}
}