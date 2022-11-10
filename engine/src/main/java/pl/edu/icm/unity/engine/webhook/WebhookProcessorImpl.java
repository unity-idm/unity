/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.webhook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.integration.Webhook;
import pl.edu.icm.unity.engine.api.integration.Webhook.WebhookHttpMethod;
import pl.edu.icm.unity.engine.api.webhook.WebhookProcessor;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
public class WebhookProcessorImpl implements WebhookProcessor
{
	private static final String BEARER_AUTH = "Bearer ";
	private static final String AUTHORIZATION_HEADER = "Authorization";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, WebhookProcessorImpl.class);
	private PKIManagement pkiMan;

	@Autowired
	public WebhookProcessorImpl(PKIManagement pkiMan)
	{
		this.pkiMan = pkiMan;
	}

	@Override
	public ClassicHttpResponse trigger(Webhook webhook, Map<String, String> params) throws EngineException
	{
		try
		{
			return webhook.httpMethod.equals(WebhookHttpMethod.GET) ? doGet(webhook, params)
					: sendPost(webhook, params);

		} catch (Exception e)
		{
			throw new EngineException("Can not execute webhook", e);
		}
	}

	private  ClassicHttpResponse doGet(Webhook webhook, Map<String, String> params)
			throws ClientProtocolException, IOException, EngineException, ParseException, URISyntaxException
	{
		URIBuilder urlBuilder = new URIBuilder(webhook.url);
		params.forEach((k, v) -> {
				urlBuilder.addParameter(k, v);	
		});
		URL url = urlBuilder.build().toURL();
		HttpClient httpClient = getSSLClient(url.toString(), webhook.truststore);
		HttpGet request = new HttpGet(url.toString());
		if (webhook.secret != null && !webhook.secret.isEmpty())
		{
			request.setHeader(AUTHORIZATION_HEADER, BEARER_AUTH + webhook.secret);
		}
		
		log.info("Request GET to " + url.toString());
		return httpClient.executeOpen(null, request, HttpClientContext.create());
	}

	private ClassicHttpResponse sendPost(Webhook webhook, Map<String, String> params)
			throws ClientProtocolException, IOException, EngineException, ParseException
	{
		HttpPost post = new HttpPost(webhook.url);
		List<NameValuePair> postParameters = new ArrayList<>();
		params.forEach((k, v) -> {
				postParameters.add(new BasicNameValuePair(k, v));
		});
		post.setEntity(new UrlEncodedFormEntity(postParameters));
		if (webhook.secret != null && !webhook.secret.isEmpty())
		{
			post.setHeader(AUTHORIZATION_HEADER, BEARER_AUTH + webhook.secret);
		}
		HttpClient httpClient = getSSLClient(webhook.url, webhook.truststore);
		log.info("Request POST to " + webhook.url + " with entity: " + EntityUtils.toString(post.getEntity()));
		return httpClient.executeOpen(null, post, HttpClientContext.create());
	}

	private HttpClient getSSLClient(String url, String customTruststore) throws EngineException
	{
		if (customTruststore != null)
		{
			DefaultClientConfiguration config = new DefaultClientConfiguration();
			config.setSslEnabled(true);
			config.setValidator(pkiMan.getValidator(customTruststore));
			return HttpUtils.createClient(url, config);
		} else
		{
			return HttpClientBuilder.create().build();
		}
	}
}
