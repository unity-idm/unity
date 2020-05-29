/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logger categories
 * @author K. Benedyczak
 */
public class Log
{
	public static final String U_SERVER = "unity.server";
	public static final String U_SERVER_CFG = "unity.server.config";
	public static final String U_SERVER_DB = "unity.server.db";
	public static final String U_SERVER_WEB = "unity.server.web";
	public static final String U_SERVER_SAML = "unity.server.saml";
	public static final String U_SERVER_OAUTH = "unity.server.oauth";
	public static final String U_SERVER_LDAP = "unity.server.ldap";
	public static final String U_SERVER_WS = "unity.server.ws";
	public static final String U_SERVER_REST = "unity.server.rest";
	public static final String U_SERVER_PAM = "unity.server.pam";
	public static final String U_SERVER_TRANSLATION = "unity.server.externaltranslation";
	public static final String U_SERVER_FIDO = "unity.server.fido";
	public static final String SECURITY = "unicore.security"; //legacy

	public static Logger getLogger(String category, Class<?> clazz)
	{
		return LogManager.getLogger(category + "." + clazz.getSimpleName());
	}
	
	public static org.apache.log4j.Logger getLegacyLogger(String category, Class<?> clazz)
	{
		return org.apache.log4j.Logger.getLogger(category + "." + clazz.getSimpleName());
	}
}
