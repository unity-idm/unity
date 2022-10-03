/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.files;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;

import java.io.IOException;
import java.net.URL;

/**
 * Wraps configuration of HTTP client which can use custom truststore and makes
 * GET connection, returning the content of downloaded file
 * @author P.Piernik
 *
 */
class RemoteFileNetworkClient
{
	private PKIManagement pkiManagement;

	RemoteFileNetworkClient(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	public byte[] download(URL url, String customTruststore, int connectionTimeout, int retriesNumber) 
			throws EngineException, IOException
	{
		HttpClient client = url.getProtocol().equals("https") ? 
				getSSLClient(url.toString(), customTruststore, retriesNumber)
				: getPlainClient(retriesNumber);
		HttpClientContext httpClientContext = HttpClientContext.create();
		httpClientContext.setRequestConfig(RequestConfig.custom()
				.setConnectTimeout(connectionTimeout)
				.build());
		return download(client, url, httpClientContext);
	}

	public byte[] download(URL url, String customTruststore) throws EngineException, IOException
	{
		HttpClient client = url.getProtocol().equals("https") ? getSSLClient(url.toString(), customTruststore)
				: HttpClientBuilder.create().build();
		return download(client, url, null);
	}
			
	private byte[] download(HttpClient client, URL url, HttpClientContext httpClientContext) throws EngineException, IOException
	{
		HttpGet request = new HttpGet(url.toString());
		HttpResponse response;
		if(httpClientContext != null)
			response = client.execute(request, httpClientContext);
		else
			response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			String body = response.getEntity().getContentLength() < 10240
					? EntityUtils.toString(response.getEntity())
					: "";

			throw new IOException("File download from " + url + " error: "
					+ response.getStatusLine().toString() + "; " + body);
		}

		return IOUtils.toByteArray(response.getEntity().getContent());
	}
	
	private CloseableHttpClient getPlainClient(int retriesNumber)
	{
		return HttpClientBuilder.create()
				.setRetryHandler(new DefaultHttpRequestRetryHandler(retriesNumber, retriesNumber > 0))
				.build();
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
			return HttpClientBuilder.create().disableAutomaticRetries()
					.build();
		}
	}

	private HttpClient getSSLClient(String url, String customTruststore, int retriesNumber) throws EngineException
	{
		if (customTruststore != null)
		{
			DefaultClientConfiguration config = new DefaultClientConfiguration();
			config.setSslEnabled(true);
			config.setValidator(pkiManagement.getValidator(customTruststore));
			config.setMaxWSRetries(retriesNumber);
			return HttpUtils.createClient(url, config);
		} else
		{
			return getPlainClient(retriesNumber);
		}
	}

}
