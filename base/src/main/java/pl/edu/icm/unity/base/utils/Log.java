/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Logger categories & JUL redirection setup
 * @author K. Benedyczak
 */
public class Log
{
	private static final String PFX = "unity.server.";
	public static final String U_SERVER_CORE = PFX + "core";
	public static final String U_SERVER_CFG = PFX + "config";
	public static final String U_SERVER_DB = PFX + "db";
	public static final String U_SERVER_WEB = PFX + "web";
	public static final String U_SERVER_SAML = PFX + "saml";
	public static final String U_SERVER_OAUTH = PFX + "oauth";
	public static final String U_SERVER_LDAP = PFX + "ldap";
	public static final String U_SERVER_WS = PFX + "ws";
	public static final String U_SERVER_REST = PFX + "rest";
	public static final String U_SERVER_SCIM = PFX + "scim";
	public static final String U_SERVER_PAM = PFX + "pam";
	public static final String U_SERVER_OTP = PFX + "otp";
	public static final String U_SERVER_TRANSLATION = PFX + "externaltranslation";
	public static final String U_SERVER_FIDO = PFX + "fido";
	public static final String U_SERVER_UPMAN = PFX + "upman";
	public static final String U_SERVER_FORMS = PFX + "forms";
	public static final String U_SERVER_AUTHN = PFX + "authn";
	public static final String U_SERVER_AUDIT = PFX + "audit";
	public static final String U_SERVER_NOTIFY = PFX + "notification";
	public static final String U_SERVER_EVENT = PFX + "event";
	public static final String U_SERVER_CONFIRMATION = PFX + "confirmation";
	public static final String U_SERVER_SCRIPT = PFX + "script";
	public static final String U_SERVER_USER_IMPORT = PFX + "userimport";
	public static final String U_SERVER_BULK_OPS = PFX + "bulkops";
	public static final String BUG_CATCHER = PFX + "bug.catcher";
	public static final String U_SERVER_ATTR_INTROSPECTION = PFX + "attrintrospection";

	public static final String SECURITY = "unicore.security"; //legacy

	public static final String[] REMOTE_AUTHENTICATION_RELATED_FACILITIES = {
			U_SERVER_SAML, U_SERVER_OAUTH, U_SERVER_LDAP, U_SERVER_PAM,
			U_SERVER_TRANSLATION, U_SERVER_AUTHN};
	
	static
	{
		redirectJULToSLF();
	}
	
	public static Logger getLogger(String category, Class<?> clazz)
	{
		return LogManager.getLogger(category + "." + clazz.getSimpleName());
	}
	
	private static void redirectJULToSLF()
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}
}
