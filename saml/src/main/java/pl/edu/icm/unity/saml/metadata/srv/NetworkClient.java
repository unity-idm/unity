/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Wraps configuration of HTTP client which can use custom truststore 
 * and makes GET connection, returning the stream with HTTP entity.
 *
 * TODO this does not seem to be used anywhere
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
		ClassicHttpResponse response = client.executeOpen(null, request, HttpClientContext.create());
		if (response.getCode() != HttpStatus.SC_OK)
		{
			String body = "";
			try {
				body = response.getEntity().getContentLength() < 10240 ? EntityUtils
					.toString(response.getEntity()) : "";
				response.close();
			}catch(ParseException pe) {}
			throw new IOException("Metadata download from " + url + " error: "
					+ new StatusLine(response).toString() + "; " + body);
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
