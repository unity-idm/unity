/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

/**
 * Used by {@link SandboxAuthnRouter} and it is intended to be used only by
 * listener's code.
 *  
 * @author R. Krysinski
 */
public interface SandboxAuthnNotifier 
{
	void addListener(RemoteAuthnInputListener listener);

	void removeListener(RemoteAuthnInputListener listener);
	
	void addListener(AuthnResultListener listener);
	
	void removeListener(AuthnResultListener listener);
	
	public interface RemoteAuthnInputListener {
		void handle(SandboxRemoteAuthnInputEvent event);
	}
	
	public interface AuthnResultListener {
		void handle(SandboxAuthnResultEvent event);
	}	
}
