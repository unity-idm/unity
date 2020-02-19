/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * WARNING: SSL verification is turned off.
 */
public class RestAdminHttpClient
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final PerformanceTestConfig config;
	public static final MediaType TEXT_PLAIN = MediaType.get("application/json; charset=utf-8");

	public RestAdminHttpClient(PerformanceTestConfig config)
	{
		this.config = config;
	}

	public void invalidateSession(String userName)
	{
		OkHttpClient client = getInsecureHttpClient();

		RequestBody body = RequestBody.create(TEXT_PLAIN, userName);
		String url = config.unityBaseURL + "/rest-admin/v1/triggerEvent/invalidate_session";
		Request request = new Request.Builder()
				.url(url)
				.addHeader("Authorization", Credentials.basic(config.restUserName, config.restUserPasswd))
				.post(body)
				.build();

		try (Response response = client.newCall(request).execute())
		{
			if (!response.isSuccessful())
				throw new IllegalStateException("Unable to invalidate session for " + userName + ", reason: " + response.code());
		} catch (IOException e)
		{
			throw new IllegalStateException("Failed to invalidate session for " + userName, e);
		}
	}

	private OkHttpClient getInsecureHttpClient()
	{
		return getInsecureHttpClientBuilder()
				.addInterceptor(new LoggingInterceptor())
				.build();
	}
	
	private OkHttpClient.Builder getInsecureHttpClientBuilder()
	{
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
		{
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws CertificateException
			{
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws CertificateException
			{
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers()
			{
				return new java.security.cert.X509Certificate[]
				{};
			}
		} };
		try
		{
			SSLContext trustAllSslContext = SSLContext.getInstance("SSL");
			trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();
			return new OkHttpClient.Builder()
					.hostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0]);
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static class NoopHostnameVerifier implements HostnameVerifier
	{
		static final NoopHostnameVerifier INSTANCE = new NoopHostnameVerifier();

		@Override
		public boolean verify(final String s, final SSLSession sslSession)
		{
			return true;
		}
	}
	
	class LoggingInterceptor implements Interceptor
	{
		@Override
		public Response intercept(Chain chain) throws IOException
		{
			Request request = chain.request();

			long t1 = System.nanoTime();
			LOG.debug("OkHttp {}", String.format("Sending request %s on %s%n%s", request.url(), chain.connection(),
					request.headers()));

			Response response = chain.proceed(request);

			long t2 = System.nanoTime();
			LOG.debug("OkHttp {}", String.format("Received response for %s in %.1fms%n%s", response.request().url(),
					(t2 - t1) / 1e6d, response.headers()));

			return response;
		}
	}
}
