/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.v8;

import java.util.Objects;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public class TrustedUpstreamASBean
{
	private String clientId;
	private String clientSecret;

	private String metadataURL;

	private String issuerURI;
	private String introspectionEndpointURL;
	private String certificate;
	private ServerHostnameCheckingMode clientHostnameChecking;
	private String clientTrustStore;
	

	public TrustedUpstreamASBean()
	{
		clientHostnameChecking = ServerHostnameCheckingMode.FAIL;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(certificate, clientId, clientSecret, introspectionEndpointURL, issuerURI, metadataURL);
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
		TrustedUpstreamASBean other = (TrustedUpstreamASBean) obj;
		return Objects.equals(certificate, other.certificate) && Objects.equals(clientId, other.clientId)
				&& Objects.equals(clientSecret, other.clientSecret)
				&& Objects.equals(introspectionEndpointURL, other.introspectionEndpointURL)
				&& Objects.equals(issuerURI, other.issuerURI) && Objects.equals(metadataURL, other.metadataURL);
	}

	protected TrustedUpstreamASBean clone()
	{
		TrustedUpstreamASBean clone = new TrustedUpstreamASBean();
		clone.setClientId(clientId);
		clone.setClientSecret(clientSecret);
		clone.setCertificate(certificate);
		clone.setIntrospectionEndpointURL(introspectionEndpointURL);
		clone.setIssuerURI(issuerURI);
		clone.setMetadataURL(metadataURL);
		clone.setClientHostnameChecking(clientHostnameChecking);
		clone.setClientTrustStore(clientTrustStore);
		return clone;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public String getClientSecret()
	{
		return clientSecret;
	}

	public void setClientSecret(String clientSecret)
	{
		this.clientSecret = clientSecret;
	}

	public String getMetadataURL()
	{
		return metadataURL;
	}

	public void setMetadataURL(String metadataURL)
	{
		this.metadataURL = metadataURL;
	}

	public String getIssuerURI()
	{
		return issuerURI;
	}

	public void setIssuerURI(String issuerURI)
	{
		this.issuerURI = issuerURI;
	}

	public String getIntrospectionEndpointURL()
	{
		return introspectionEndpointURL;
	}

	public void setIntrospectionEndpointURL(String introspectionEndpointURL)
	{
		this.introspectionEndpointURL = introspectionEndpointURL;
	}

	public String getCertificate()
	{
		return certificate;
	}

	public void setCertificate(String credential)
	{
		this.certificate = credential;
	}

	public ServerHostnameCheckingMode getClientHostnameChecking()
	{
		return clientHostnameChecking;
	}

	public void setClientHostnameChecking(ServerHostnameCheckingMode clientHostnameChecking)
	{
		this.clientHostnameChecking = clientHostnameChecking;
	}

	public String getClientTrustStore()
	{
		return clientTrustStore;
	}

	public void setClientTrustStore(String clientTrustStore)
	{
		this.clientTrustStore = clientTrustStore;
	}

}
