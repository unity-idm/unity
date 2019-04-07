/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.ldap.client.web;

/**
 * Ldap server specification
 * @author P.Piernik
 *
 */
public class ServerSpecification
{
	private String server;
	private int port;

	public ServerSpecification()
	{
		this("", 0);
	}

	public ServerSpecification(String server, int port)
	{
		this.server = server;
		this.port = port;
	}

	public String getServer()
	{
		return server;
	}

	public void setServer(String server)
	{
		this.server = server;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}
}