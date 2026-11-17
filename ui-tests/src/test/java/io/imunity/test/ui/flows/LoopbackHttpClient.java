package io.imunity.test.ui.flows;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.Constants;

final class LoopbackHttpClient
{
	private final SSLSocketFactory sockets;

	LoopbackHttpClient() throws Exception
	{
		KeyStore trust = KeyStore.getInstance("JKS");
		try (InputStream input = Files.newInputStream(Path.of("target/ui-fixtures/demoTruststore.jks")))
		{
			trust.load(input, "the!unity".toCharArray());
		}
		TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagers.init(trust);
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustManagers.getTrustManagers(), null);
		sockets = context.getSocketFactory();
	}

	Result post(String url, Map<String, String> parameters) throws Exception
	{
		return post(url, parameters, Map.of());
	}

	Result post(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception
	{
		return request(url, "POST", headers, parameters.entrySet().stream()
				.map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
				.collect(Collectors.joining("&")));
	}

	Result get(String url, Map<String, String> headers) throws Exception
	{
		return request(url, "GET", headers, null);
	}

	private Result request(String url, String method, Map<String, String> headers, String body) throws Exception
	{
		URI uri = URI.create(url);
		if (!"https".equals(uri.getScheme()) || !"localhost".equals(uri.getHost()) || uri.getPort() != 2443)
			throw new IllegalArgumentException("Only the disposable loopback server is supported");
		HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection(java.net.Proxy.NO_PROXY);
		connection.setSSLSocketFactory(sockets);
		connection.setHostnameVerifier((host, session) -> "localhost".equals(host));
		connection.setConnectTimeout(10_000);
		connection.setReadTimeout(10_000);
		connection.setRequestMethod(method);
		headers.forEach(connection::setRequestProperty);
		try
		{
			if (body != null)
			{
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				try (var output = connection.getOutputStream())
				{
					output.write(body.getBytes(StandardCharsets.UTF_8));
				}
			}
			int status = connection.getResponseCode();
			try (InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream())
			{
				return new Result(status, Constants.MAPPER.readTree(input));
			}
		} finally
		{
			connection.disconnect();
		}
	}

	static String encode(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	record Result(int status, JsonNode body)
	{
	}
}
