/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.imunity.tanl.x509.X509CertChainValidatorExt;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.imunity.tanl.x509.X509Credential;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.config.UnityHttpServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.URLFactory;

public class JettyServerFeaturesTest 
{
	private JettyServer httpServer;
	
	@AfterEach
	public void clear() throws EngineException
	{
		httpServer.stop();
	}
	
	@Test
	void shouldReturnConfiguredHeaders() throws Exception
	{
		buildHttpServer();
		
		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/test");
		handler.addServlet(new ServletHolder(new SimpleServlet()), "/*");
		httpServer.start();
		httpServer.deployHandler(handler, "sys:test");
		URL url = httpServer.getUrls()[0];
		String baseURL = "https://127.0.0.1:" + url.getPort();

		HttpClient client = createClient(baseURL + "/test/1");
		HttpResponse response = makeGetRequest(client, baseURL + "/test/1");

		assertThat(response.getCode()).isEqualTo(200);
		assertThat(response.getHeader("Strict-Transport-Security").getValue()).isEqualTo("max-age=31536000; includeSubDomains");
		assertThat(response.getHeader("X-Frame-Options").getValue()).isEqualTo("ALLOW-FROM example.com");
	}

	@Test
	void shouldBlockTraceMethod() throws Exception
	{
		buildHttpServer();
		
		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/test-trace");
		handler.addServlet(new ServletHolder(new SimpleServlet()), "/*");
		httpServer.start();

		httpServer.deployHandler(handler, "sys:test");
		URL url = httpServer.getUrls()[0];
		String baseURL = "https://127.0.0.1:" + url.getPort();

		HttpClient client = createClient(baseURL + "/test-trace/1");
		HttpResponse response = makeTraceRequest(client, baseURL + "/test-trace/1");

		assertThat(response.getCode()).isEqualTo(405);
	}
	
	@Test
	void shouldNotExposeExceptionDetailsOnErrorPage() throws Exception
	{
		buildHttpServer();

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/test-error");
		handler.addServlet(new ServletHolder(new FailingServlet()), "/*");
		httpServer.start();
		httpServer.deployHandler(handler, "sys:test");

		ErrorResponse response = makeGetRequestReadingBody(httpServerURL() + "/test-error/1");

		assertThat(response.code()).isEqualTo(500);
		assertNoInternalDetailsIn(response.body());
	}

	@Test
	void shouldNotExposeStackTraceOnErrorPageOfWebAppContext() throws Exception
	{
		buildHttpServer();

		WebAppContext handler = new WebAppContext();
		handler.setBaseResource(new URLResourceFactory().newResource(new File("src/test/resources").toURI().toURL()));
		handler.setContextPath("/test-webapp-error");
		handler.addServlet(new ServletHolder(new FailingServlet()), "/*");
		httpServer.start();
		httpServer.deployHandler(handler, "sys:test");

		ErrorResponse response = makeGetRequestReadingBody(httpServerURL() + "/test-webapp-error/1");

		assertThat(response.code()).isEqualTo(500);
		assertNoInternalDetailsIn(response.body());
	}

	private static void assertNoInternalDetailsIn(String body)
	{
		assertThat(body)
				.doesNotContain(FailingServlet.INTERNAL_DETAIL)
				.doesNotContain("java.lang.")
				.doesNotContain("at pl.edu");
	}

	private String httpServerURL()
	{
		return "https://127.0.0.1:" + httpServer.getUrls()[0].getPort();
	}

	private ErrorResponse makeGetRequestReadingBody(String url) throws Exception
	{
		HttpClient client = createClient(url);
		try (ClassicHttpResponse response = client.executeOpen(null, new HttpGet(url), HttpClientContext.create()))
		{
			return new ErrorResponse(response.getCode(), EntityUtils.toString(response.getEntity()));
		}
	}

	private record ErrorResponse(int code, String body)
	{
	}

	private static class FailingServlet extends HttpServlet
	{
		private static final String INTERNAL_DETAIL = "SECRET-INTERNAL-DETAIL";

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		{
			throw new IllegalStateException(INTERNAL_DETAIL);
		}
	}

	private void buildHttpServer() throws IOException
	{
		UnityHttpServerConfiguration httpConfig = prepareHttpServerConfig();
		IAuthnAndTrustConfiguration securityConfiguration = prepareSSLCredential();
		httpServer = new JettyServer(httpConfig, "webContents", null, securityConfiguration, 
				new URL[] { URLFactory.of("https://0.0.0.0:0")});
	}

	private IAuthnAndTrustConfiguration prepareSSLCredential() throws IOException
	{
		Properties securityProps = new Properties();
		securityProps.load(new StringReader(
				"""
				unity.pki.credentials.MAIN.format=pkcs12
				unity.pki.credentials.MAIN.path=src/test/resources/pki/demoKeystore.p12
				unity.pki.credentials.MAIN.keyAlias=unity-demo
				unity.pki.credentials.MAIN.password=the!unity

				unity.pki.truststores.MAIN.type=keystore
				unity.pki.truststores.MAIN.keystorePath=src/test/resources/pki/demoTruststore.jks
				unity.pki.truststores.MAIN.keystorePassword=the!unity
				unity.pki.truststores.MAIN.keystoreFormat=JKS
				"""
		));
		TruststoreProperties truststoreProps = new TruststoreProperties(securityProps,
				List.of(),
				"unity.pki.truststores.MAIN.");
		CredentialProperties keystorePros = new CredentialProperties(securityProps,
				"unity.pki.credentials.MAIN.");
		return new DefaultAuthnAndTrustConfiguration(
				truststoreProps.getValidator(),
				keystorePros.getCredential());
	}

	private UnityHttpServerConfiguration prepareHttpServerConfig() throws IOException
	{
		Properties httpConfigProps = new Properties();
		httpConfigProps.load(new StringReader(
"""
unityServer.core.httpServer.port=0
unityServer.core.httpServer.host=0.0.0.0
unityServer.core.httpServer.advertisedHost=127.0.0.1
unityServer.core.httpServer.fastRandom=true
unityServer.core.httpServer.enableHsts=true
unityServer.core.httpServer.xFrameAllowed=example.com
unityServer.core.httpServer.xFrameOptions=allowFrom
"""
		));
		return new UnityHttpServerConfiguration(httpConfigProps);
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
	
	private HttpResponse makeGetRequest(HttpClient client, String url) throws Exception
	{
		HttpGet get = new HttpGet(url);
		try (ClassicHttpResponse response = client.executeOpen(null, get, HttpClientContext.create()))
		{
			return response;
		}
	}
	
	private HttpResponse makeTraceRequest(HttpClient client, String url) throws Exception
	{
		HttpTrace trace = new HttpTrace(url);
		try (ClassicHttpResponse response = client.executeOpen(null, trace, HttpClientContext.create()))
		{
			return response;
		}
	}
}
