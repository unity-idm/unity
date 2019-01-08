/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Wraps configuration of HTTP client which can use custom truststore 
 * and makes GET connection, returning the stream with HTTP entity.
 * 
 * @author K. Benedyczak
 */
public class NetworkClient
{
	private PKIManagement pkiManagement;
	
	public NetworkClient(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}


	public InputStream download(String url, String customTruststore) throws EngineException,
			IOException
	{
		HttpClient client = url.startsWith("https:") ? getSSLClient(url, customTruststore)
				: HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			String body = response.getEntity().getContentLength() < 10240 ? EntityUtils
					.toString(response.getEntity()) : "";

			throw new IOException("Metadata download from " + url + " error: "
					+ response.getStatusLine().toString() + "; " + body);
		}

		return response.getEntity().getContent();
	}
	

	private HttpClient getSSLClient(String url, String customTruststore) throws EngineException
	{
		if (customTruststore != null)
		{
			DefaultClientConfiguration config = new DefaultClientConfiguration();
			config.setSslEnabled(true);
			config.setValidator(pkiManagement.getValidator(customTruststore));
			return HttpUtils.createClient(url, config);
		} else
		{
			return HttpClientBuilder.create().build();
		}
	}
}
