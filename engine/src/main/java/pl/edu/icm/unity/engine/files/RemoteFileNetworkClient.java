/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.files;

import static eu.unicore.util.httpclient.HttpClientProperties.CONNECT_TIMEOUT;
import static eu.unicore.util.httpclient.HttpClientProperties.SO_TIMEOUT;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Preconditions;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.helpers.ssl.SSLTrustManagerWithHostnameChecking;
import eu.emi.security.authn.x509.impl.HostnameMismatchCallback2;
import eu.unicore.util.httpclient.CustomSSLConnectionSocketFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.EmptyHostnameVerifier;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
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
	private static final long MAX_BODY_SIZE_TO_LOG = 10240;
	private PKIManagement pkiManagement;

	RemoteFileNetworkClient(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	ContentsWithType download(URL url, String customTruststore, Duration connectionAndSocketReadTimeout, int retriesNumber)
			throws EngineException, IOException
	{
		HttpClient client = new ApacheHttpClientBuilder(pkiManagement)
				.withConnectionAndSocketReadTimeout(connectionAndSocketReadTimeout)
				.withCustomTruststore(customTruststore)
				.withRetriesNumber(retriesNumber)
				.withURL(url)
				.build();
		return download(client, url);
	}

	ContentsWithType download(URL url, String customTruststore) throws EngineException, IOException
	{
		HttpClientProperties properties = new DefaultClientConfiguration().getHttpClientProperties();
		HttpClient client = new ApacheHttpClientBuilder(pkiManagement)
				.withCustomTruststore(customTruststore)
				.withSocketReadTimeout(properties.getIntValue(SO_TIMEOUT))
				.withConnectionTimeout(properties.getIntValue(CONNECT_TIMEOUT))
				.withDefaultRetries()
				.withURL(url)
				.build();
		return download(client, url);
	}
			
	private ContentsWithType download(HttpClient client, URL url) throws EngineException, IOException
	{
		HttpGet request = new HttpGet(url.toString());
		HttpResponse response = client.execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK)
		{
			StringBuilder errorMessage = new StringBuilder()
					.append("File download from ")
					.append(url)
					.append(", error: ")
					.append(response.getStatusLine().toString());
			if (statusCode != HttpStatus.SC_NOT_FOUND && statusCode != HttpStatus.SC_FORBIDDEN)
			{
				String body = response.getEntity().getContentLength() < MAX_BODY_SIZE_TO_LOG 
						? EntityUtils.toString(response.getEntity())
						: "HTTP body too large";
				errorMessage.append(", body: ").append(body);
			}
			throw new IOException(errorMessage.toString());
		}
		Header contentTypeHeader = response.getFirstHeader("Content-Type");
		String contentType = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
		return new ContentsWithType(IOUtils.toByteArray(response.getEntity().getContent()), contentType);
	}
	
	static class ContentsWithType
	{
		final byte[] contents;
		final String mimeType;

		ContentsWithType(byte[] contents, String mimeType)
		{
			this.contents = contents;
			this.mimeType = mimeType;
		}
	}
	
	private static class ApacheHttpClientBuilder
	{
		private static final int DEFAULT_RETRY_MECHANISM = -1;
		private final PKIManagement pkiManagement;
		
		private URL url;
		private String customTruststore; 
		private Integer connectionTimeout;
		private Integer socketReadTimeout;
		private int retriesNumber = DEFAULT_RETRY_MECHANISM;

		ApacheHttpClientBuilder(PKIManagement pkiManagement)
		{
			this.pkiManagement = pkiManagement;
		}
		
		public ApacheHttpClientBuilder withDefaultRetries()
		{
			this.retriesNumber = DEFAULT_RETRY_MECHANISM;
			return this;
		}

		ApacheHttpClientBuilder withURL(URL url)
		{
			this.url = url;
			return this;
		}
		
		ApacheHttpClientBuilder withCustomTruststore(String customTruststore)
		{
			this.customTruststore = customTruststore;
			return this;
		}
		
		ApacheHttpClientBuilder withConnectionTimeout(int connectionTimeout)
		{
			this.connectionTimeout = connectionTimeout;
			return this;
		}
		
		ApacheHttpClientBuilder withSocketReadTimeout(int socketReadTimeout)
		{
			this.socketReadTimeout = socketReadTimeout;
			return this;
		}
		
		ApacheHttpClientBuilder withConnectionAndSocketReadTimeout(Duration connectionAndSocketReadTimeout)
		{
			this.connectionTimeout = (int) connectionAndSocketReadTimeout.toMillis();
			this.socketReadTimeout = (int) connectionAndSocketReadTimeout.toMillis();
			return this;
		}
		
		ApacheHttpClientBuilder withRetriesNumber(int retriesNumber)
		{
			this.retriesNumber = retriesNumber;
			return this;
		}
		
		HttpClient build() throws EngineException
		{
			Preconditions.checkNotNull(url, "url must not provided");
			Preconditions.checkNotNull(connectionTimeout, "connectionTimeout must not provided");
			Preconditions.checkNotNull(socketReadTimeout, "socketReadTimeout must not provided");
			
			HttpClientBuilder builder = HttpClientBuilder.create();
			
			if (retriesNumber == 0)
			{
				builder.disableAutomaticRetries();
				
			} else if (retriesNumber > 0)
			{
				builder.setRetryHandler(new DefaultHttpRequestRetryHandler(retriesNumber, true));
				
			} else if (retriesNumber == DEFAULT_RETRY_MECHANISM)
			{
				builder.setRetryHandler(DefaultHttpRequestRetryHandler.INSTANCE);
			}
			
			builder.setDefaultRequestConfig(RequestConfig.custom()
					.setSocketTimeout(socketReadTimeout)
					.setConnectTimeout(connectionTimeout)
					.setConnectionRequestTimeout(connectionTimeout)
					.build());
			
			if (customTruststore != null && url.getProtocol().equals("https"))
			{
				X509CertChainValidatorExt validator = pkiManagement.getValidator(customTruststore);
				SSLContext sslContext = new SSLContextBuilder(validator).build();
				CustomSSLConnectionSocketFactory sslsf = new CustomSSLConnectionSocketFactory(sslContext,
						new EmptyHostnameVerifier(), socketReadTimeout);
				builder.setSSLSocketFactory(sslsf);
			}
			
			return builder.build();
		}
		
	}
	
	private static class SSLContextBuilder
	{
		private static final String TLSV_1_2 = "TLSv1.2";

		private final X509CertChainValidatorExt validator;
		
		SSLContextBuilder(X509CertChainValidatorExt validator)
		{
			this.validator = validator;
		}

		SSLContext build()
		{
			try
			{
				SSLContext sslContext = SSLContext.getInstance(TLSV_1_2);
				HostnameMismatchCallback2 hostnameVerificationCallback = new HostnameMismatchCallbackImpl(
						ServerHostnameCheckingMode.NONE);
				X509TrustManager tm = new SSLTrustManagerWithHostnameChecking(validator, hostnameVerificationCallback);
				sslContext.init(null, new TrustManager[] { tm }, null);
				return sslContext;

			} catch (NoSuchAlgorithmException | KeyManagementException e)
			{
				throw new IllegalStateException("Could not build SSLContext", e);
			}
		}
	}

}
