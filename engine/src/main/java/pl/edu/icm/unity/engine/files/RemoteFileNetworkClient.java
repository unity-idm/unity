/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.files;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
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

	public byte[] download(URL url, String customTruststore) throws EngineException, IOException
	{
		HttpClient client = url.getProtocol().equals("https") ? getSSLClient(url.toString(), customTruststore)
				: HttpClientBuilder.create().build();
		return download(client, url);
	}
			
	private byte[] download(HttpClient client, URL url) throws EngineException, IOException
	{
		HttpGet request = new HttpGet(url.toString());
		HttpResponse response = client.execute(request);
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
