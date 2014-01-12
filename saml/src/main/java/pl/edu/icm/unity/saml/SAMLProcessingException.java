/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

/**
 * Signals an problem in SAML handling. Such errors should be signaled only 
 * when error can not be returned via SAML. Typical examples are problems with parsing of the request,
 * no return URL in the request or unexpected internal server's problems (bugs?) which occur during the
 * request processing.
 *   
 * The action taken when this exception is thrown depends on the SAML binding. For the web bindings 
 * the error page should be presented. For SOAP bindings a SOAP fault should be sent.
 * @author K. Benedyczak
 */
public class SAMLProcessingException extends Exception
{
	public SAMLProcessingException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public SAMLProcessingException(String message)
	{
		super(message);
	}

	public SAMLProcessingException(Throwable cause)
	{
		super(cause);
	}
}
