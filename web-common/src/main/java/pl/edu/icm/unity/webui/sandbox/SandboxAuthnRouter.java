/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;

/**
 * Main sandbox authentication router interface. It's divided into 
 * {@link SandboxAuthnNotifier} to have a clear separation between 
 * event generators and listeners. 
 * 
 * @author R. Krysinski
 */
public interface SandboxAuthnRouter extends SandboxAuthnNotifier
{
	void firePartialEvent(SandboxAuthnEvent event);
	void fireCompleteEvent(AuthenticatedEntity entity);
}
