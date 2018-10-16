/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeUtility;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;

/**
 * Wrapper of {@link HTTPRequest} with customized TLS setup.
 * <p>
 * Implementation note: this class extends {@link HTTPRequest} to be API compatible and have override annotations
 * on all methods so that when the upstream class API changes we have compilation time error. The super
 * class object is not fully initialized and this implementation works as a wrapper delegating all operations
 * to the wrapped object. The change is in {@link #toHttpURLConnection()}. Unfortunately also the {@link #send()}
 * needs to be replicated to call the correct {@link #toHttpURLConnection()} implementation.
 * 
 * @author K. Benedyczak
 */
public class CustomHTTPSRequest extends HTTPRequest
{
	private HTTPRequest wrapped;
	private SSLSocketFactory factory;
	private HostnameVerifier verifier;
	
	public CustomHTTPSRequest(HTTPRequest wrapped, X509CertChainValidator validator,
			ServerHostnameCheckingMode mode)
	{
		super(wrapped.getMethod(), wrapped.getURL());
		this.wrapped = wrapped;
		if (validator != null)
			factory = SocketFactoryCreator.getSocketFactory(null, validator);
		verifier = new CanlHostnameVerifierJDK(mode);
	}
	
	@Override
	public HttpURLConnection toHttpURLConnection() throws IOException
	{
		URL finalURL = wrapped.getURL();

		HTTPRequest.Method method = wrapped.getMethod();
		String query = wrapped.getQuery();
		
		if (query != null && (method.equals(
				HTTPRequest.Method.GET) || wrapped.getMethod().equals(Method.DELETE))) {

			// Append query string
			StringBuilder sb = new StringBuilder(wrapped.getURL().toString());
			sb.append('?');
			sb.append(query);

			try {
				finalURL = new URL(sb.toString());

			} catch (MalformedURLException e) {

				throw new IOException("Couldn't append query string: " + e.getMessage(), e);
			}
		}

		HttpsURLConnection conn = (HttpsURLConnection)finalURL.openConnection();
		if (factory != null)
			conn.setSSLSocketFactory(factory);
		conn.setHostnameVerifier(verifier);

		if (wrapped.getAuthorization() != null)
			conn.setRequestProperty("Authorization", wrapped.getAuthorization());

		conn.setRequestMethod(method.name());
		for (Map.Entry<String, List<String>> header: wrapped.getHeaderMap().entrySet())
			conn.setRequestProperty(header.getKey(), header.getValue().get(0));

		if (method.equals(HTTPRequest.Method.POST) || method.equals(Method.PUT)) {

			conn.setDoOutput(true);

			if (wrapped.getContentType() != null)
			{
				wrapped.getContentType().setParameter("charset", 
						MimeUtility.mimeCharset(StandardCharsets.UTF_8.name()));
				conn.setRequestProperty("Content-Type", wrapped.getContentType().toString());
			}

			if (query != null) {
				OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), 
						StandardCharsets.UTF_8);
				writer.write(query);
				writer.close();
			}
		}
		return conn;
	}


	@Override
	public HTTPResponse send() throws IOException
	{
		HttpURLConnection conn = toHttpURLConnection();

		int statusCode;

		BufferedReader reader;

		try {
			
			// Open a connection, then send method and headers
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), 
					StandardCharsets.UTF_8));

			// The next step is to get the status
			statusCode = conn.getResponseCode();

		} catch (IOException e) {

			// HttpUrlConnection will throw an IOException if any
			// 4XX response is sent. If we request the status
			// again, this time the internal status will be
			// properly set, and we'll be able to retrieve it.
			statusCode = conn.getResponseCode();

			if (statusCode == -1 || conn.getErrorStream() == null) {
				// Rethrow IO exception
				throw e;
			} else {
				// HTTP status code indicates the response got
				// through, read the content but using error
				// stream
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(),
						StandardCharsets.UTF_8));
			}
		}

		StringBuilder body = new StringBuilder();


		try {
			String line;

			while ((line = reader.readLine()) != null) {

				body.append(line);
				body.append(System.getProperty("line.separator"));
			}

			reader.close();

		} finally {
			conn.disconnect();
		}


		HTTPResponse response = new HTTPResponse(statusCode);

		String location = conn.getHeaderField("Location");

		if (location != null) {

			try {
				response.setLocation(new URI(location));

			} catch (URISyntaxException e) {

				throw new IOException("Couldn't parse Location header: " + e.getMessage(), e);
			}
		}


		try {
			response.setContentType(conn.getContentType());

		} catch (ParseException e) {

			throw new IOException("Couldn't parse Content-Type header: " + e.getMessage(), e);
		}


		response.setCacheControl(conn.getHeaderField("Cache-Control"));

		response.setPragma(conn.getHeaderField("Pragma"));

		response.setWWWAuthenticate(conn.getHeaderField("WWW-Authenticate"));

		String bodyContent = body.toString();

		if (! bodyContent.isEmpty())
			response.setContent(bodyContent);


		return response;
	}
	
	
	@Override
	public Method getMethod()
	{
		return wrapped.getMethod();
	}

	@Override
	public URL getURL()
	{
		return wrapped.getURL();
	}

	@Override
	public void ensureMethod(Method expectedMethod) throws ParseException
	{
		wrapped.ensureMethod(expectedMethod);
	}

	@Override
	public String getAuthorization()
	{
		return wrapped.getAuthorization();
	}

	@Override
	public void setAuthorization(String authz)
	{
		wrapped.setAuthorization(authz);
	}

	@Override
	public String getQuery()
	{
		return wrapped.getQuery();
	}

	@Override
	public void setQuery(String query)
	{
		wrapped.setQuery(query);
	}

	@Override
	public Map<String, List<String>> getQueryParameters()
	{
		return wrapped.getQueryParameters();
	}

	@Override
	public JSONObject getQueryAsJSONObject() throws ParseException
	{
		return wrapped.getQueryAsJSONObject();
	}
	
	@Override
	public Map<String,List<String>> getHeaderMap() 
	{
		return wrapped.getHeaderMap();
	}
	
	@Override
	public void setHeader(final String name, final String... values) 
	{
		wrapped.setHeader(name, values);
	}
}
