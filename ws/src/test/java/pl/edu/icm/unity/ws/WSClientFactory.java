/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package pl.edu.icm.unity.ws;

import java.net.MalformedURLException;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.xmlbeans.XmlBeansDataBinding;
import org.apache.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Helper to create web service clients using CXF. This class will configure 
 * the client using the configuration provided as {@link IClientConfiguration},
 * setting SSL, SSL authN, HTTP authN, extra HTTP settings etc. as configured.
 * 
 * @author schuller
 * @see HttpUtils
 */
public class WSClientFactory {

	protected static final Logger logger = Log.getLogger(Log.CLIENT, WSClientFactory.class);
	
	protected IClientConfiguration securityProperties;
	
	/**
	 * @param securityCfg
	 */
	public WSClientFactory(IClientConfiguration securityCfg)
	{
		if (securityCfg == null)
			throw new IllegalArgumentException("IAuthenticationConfiguration can not be null");
		if (securityCfg.getHttpClientProperties() == null)
			throw new IllegalArgumentException("HTTP settings can not be null");
		this.securityProperties = securityCfg;
	}

	/**
	 * 
	 * Create a proxy for the plain web service at the given URL, 
	 * i.e. not using ws-addressing
	 * 
	 * @param iFace
	 * @param url
	 * @param sec
	 * @return a proxy for the service defined by the interface iFace
	 * @throws MalformedURLException 
	 * @throws Exception
	 */
	public synchronized <T> T createPlainWSProxy(Class<T> iFace, String url) 
			throws MalformedURLException
	{
		JaxWsProxyFactoryBean factory=new JaxWsProxyFactoryBean();
		factory.setAddress(url);
		AbstractDataBinding binding=getBinding(iFace);
		logger.debug("Using databinding "+binding.getClass().getName());
		factory.setDataBinding(binding);
		factory.getInInterceptors().add(new LoggingInInterceptor());
		factory.getOutInterceptors().add(new LoggingOutInterceptor());
		T proxy=factory.create(iFace);
		setupProxy(proxy, url);
		
		return proxy;
	}
	
	/**
	 * Configure the client proxy class: sets up security (SSL/HTTP authn),
	 * Gzip compression, HTTP proxy, HTTP timeouts
	 *  
	 * @param client the Proxy to be configured.
	 * @param cnf Security configuration.
	 * @param properties
	 * @param uri
	 */
	protected void setupWSClientProxy(Client client, String uri)
	{
		HTTPConduit http = (HTTPConduit) client.getConduit();
		setupHTTPParams(http);
	}

	/**
	 * helper method to setup client-side HTTP settings (HTTP auth, TLS, timeouts, proxy, etc)
	 * @param http
	 */
	public void setupHTTPParams(HTTPConduit http){
		
		// HTTP auth
		if(securityProperties.doHttpAuthn()){
			AuthorizationPolicy httpAuth=new AuthorizationPolicy();
			httpAuth.setUserName(securityProperties.getHttpUser());
			httpAuth.setPassword(securityProperties.getHttpPassword());
			http.setAuthorization(httpAuth);
		}
		
		// TLS
		TLSClientParameters params = new TLSClientParameters();
		params.setSSLSocketFactory(new MySSLSocketFactory(securityProperties));
		params.setDisableCNCheck(true);
		http.setTlsClientParameters(params);
	}
	

	/**
	 * Configure the XFire proxy: sets up security (SSL/HTTP authn),
	 * Gzip compression, HTTP proxy. 
	 *  
	 * @param proxy Proxy to be configured.
	 * @param cnf Security configuration.
	 */
	protected void setupProxy(Object proxy, String uri)
	{
		setupWSClientProxy(getWSClient(proxy), uri);
	}
	
	/**
	 * get the (implementation-specific) client object
	 * @param proxy - the proxy object
	 */
	public static Client getWSClient(Object proxy)
	{
		return ClientProxy.getClient(proxy);
	}

	
	public static AbstractDataBinding getBinding(Class<?>clazz){
		return new XmlBeansDataBinding();
	}
}
