/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import javax.servlet.http.HttpServlet;

/**
 * Awaits OAuth responses and handles them.
 * 
 * @author K. Benedyczak
 */
public class ResponseConsumerServlet extends HttpServlet
{
	public static final String PATH = "/oauth2ResponseConsumer";
	
	private OAuthContextsManagement contextManagement;

	public ResponseConsumerServlet(OAuthContextsManagement contextManagement)
	{
		this.contextManagement = contextManagement;
	}
	
	
}
