/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

/**
 * Extended by {@link SandboxAuthnRouter} - contains the code which is interested with results of sandbox authentication
 *  
 * @author R. Krysinski
 */
public interface SandboxAuthnNotifier 
{
	void addListener(AuthnResultListener listener);
	void removeListener(AuthnResultListener listener);
	
	public interface AuthnResultListener 
	{
		void onSandboxAuthnResult(SandboxAuthnEvent event);
	}	
}
