/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Tests Jetty server features
 * 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/dosTest.conf" })
public class TestJettyServer 
{
	@Autowired
	protected JettyServer httpServer;
	@Autowired
	@Qualifier("insecure")
	protected ServerManagement insecureServerMan;
	
	@Before
	public void clearDB() throws Exception
	{
		insecureServerMan.resetDatabase();
	}
	
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
		String baseURL = "https://127.0.0.1:" + url.getPort();
		int THREADS = 20;
		try
		{
			DOSser[] th = new DOSser[THREADS];
			for (int i=0; i<THREADS; i++)
			{
				HttpClient client = createClient(baseURL + "/test/1");
				th[i] = new DOSser(client, baseURL);
				th[i].start();
			}
			
			int killed = 0;
			for (int i=0; i<THREADS; i++)
			{
				th[i].join();
				if (th[i].wasDenied)
					killed ++;
			}
			
			assertThat("Killed: "+ killed, killed > 3, is(true));
		
		} finally
		{
			httpServer.stop();
		}
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
