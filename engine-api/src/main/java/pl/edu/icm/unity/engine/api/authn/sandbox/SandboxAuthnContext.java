/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

public interface SandboxAuthnContext
{
	RemotelyAuthenticatedPrincipal getAuthnContext();
	Exception getAuthnException();
	String getLogs();
}
