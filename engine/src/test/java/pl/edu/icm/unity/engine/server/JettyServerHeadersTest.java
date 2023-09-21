/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.ServerManagement;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/jettyHeadersTest.conf" })
public class JettyServerHeadersTest 
{
	@Autowired
	protected JettyServer httpServer;
	@Autowired
	@Qualifier("insecure")
	protected ServerManagement insecureServerMan;
	
	@BeforeEach
	public void clearDB() throws Exception
	{
		insecureServerMan.resetDatabase();
	}
	
	@AfterEach
	public void clear() throws EngineException
	{
		httpServer.stop();
	}
	
	@Test
	void shouldReturnConfiguredHeaders() throws Exception
	{
		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/test");
		handler.addServlet(new ServletHolder(new SimpleServlet()), "/*");
		httpServer.start();
		httpServer.deployHandler(handler, "sys:test");
		URL url = httpServer.getUrls()[0];
		String baseURL = "https://127.0.0.1:" + url.getPort();

		HttpClient client = createClient(baseURL + "/test/1");
		HttpResponse response = makeRequest(client, baseURL + "/test/1");

		assertThat(response.getCode()).isEqualTo(200);
		assertThat(response.getHeader("Strict-Transport-Security").getValue()).isEqualTo("max-age=31536000; includeSubDomains");
		assertThat(response.getHeader("X-Frame-Options").getValue()).isEqualTo("ALLOW-FROM example.com");
	}

	private HttpClient createClient(String url) throws Exception
	{
		X509Credential cred = DBIntegrationTestBase.getDemoCredential();
		X509CertChainValidatorExt validator = DBIntegrationTestBase.getDemoValidator();
		DefaultClientConfiguration secCfg = new DefaultClientConfiguration(validator, cred);
		secCfg.getHttpClientProperties().setProperty(HttpClientProperties.CONNECT_TIMEOUT, "2000");
		secCfg.getHttpClientProperties().setProperty(HttpClientProperties.SO_TIMEOUT, "2000");

		return HttpUtils.createClient(url, secCfg);
	}
	
	private HttpResponse makeRequest(HttpClient client, String url) throws Exception
	{
		HttpGet get = new HttpGet(url);
		try (ClassicHttpResponse response = client.executeOpen(null, get, HttpClientContext.create()))
		{
			return response;
		}
	}
}
