/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.exceptions.EngineException;
import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Tests Jetty server features
 * 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-dos.xml"})
@ActiveProfiles("test")
public class TestJettyServer 
{
	@Autowired
	protected JettyServer httpServer;
	
	@After
	public void clear() throws EngineException
	{
		httpServer.stop();
	}
	
	@Test
	public void shouldThrottleOnDOSAttack() throws Exception
	{
		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/test");
		handler.addServlet(new ServletHolder(new SimpleServlet()), "/*");
		httpServer.start();
		httpServer.deployHandler(handler);
		URL url = httpServer.getUrls()[0];
		int THREADS = 20;
		try
		{
			HttpClient client = createClient(url.toString() + "/test/1");
			
			DOSser[] th = new DOSser[THREADS];
			for (int i=0; i<THREADS; i++)
			{
				th[i] = new DOSser(client, url.toString());
				th[i].start();
			}
			
			int killed = 0;
			for (int i=0; i<THREADS; i++)
			{
				th[i].join();
				if (th[i].wasDenied)
					killed ++;
			}
			
			assertThat(killed > 3, is(true));
		
		} finally
		{
			httpServer.stop();
		}
	}

	private HttpClient createClient(String url) throws Exception
	{
		X509Credential cred = new KeystoreCredential("src/test/resources/demoKeystore.p12",
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "PKCS12");
		X509CertChainValidatorExt validator = new KeystoreCertChainValidator(
				"src/test/resources/demoTruststore.jks",
				"unicore".toCharArray(), "JKS", -1);
		DefaultClientConfiguration secCfg = new DefaultClientConfiguration(validator, cred);
		secCfg.getHttpClientProperties().setProperty(HttpClientProperties.CONNECT_TIMEOUT, "2000");
		secCfg.getHttpClientProperties().setProperty(HttpClientProperties.SO_TIMEOUT, "2000");

		return HttpUtils.createClient(url, secCfg);
	}
	
	private boolean makeRequest(HttpClient client, String url) throws Exception
	{
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() == 429)
		{
			System.out.println("Our request was denied");
			return true;
		}
		return false;
	}
	
	private class DOSser extends Thread
	{
		HttpClient client;
		String url;
		boolean wasDenied = false;
		
		public DOSser(HttpClient client, String url)
		{
			this.client = client;
			this.url = url;
		}

		@Override
		public void run()
		{
			try
			{
				for (int j=0; j<10; j++)
					wasDenied |= makeRequest(client, url.toString() + "/test/1");
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
