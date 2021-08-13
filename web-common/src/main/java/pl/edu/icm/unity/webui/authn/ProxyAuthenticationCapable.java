/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Instances are capable of performing automated proxy authentication: 
 * one of authentication options is triggered automatically.
 * <p>
 * There are two variants: one triggers authentication before loading any of the UI. It is used by sign-in flows.
 * The other variant triggers authentication after loading UI. This variant is used in sign-up flows. It would be great 
 * to use the first variant in both cases, but this would require refactoring of public endpoint on which registration
 * forms are accessible (so that it is specific to registration forms only).
 * <p>
 * This feature is possible with external authenticators, which are redirect based and so 
 * does not require any user input on Unity.
 */
public interface ProxyAuthenticationCapable extends BindingAuthn
{
	/**
	 * @return true if the request was handled by the filter, false if the filter's chain
	 * should be processed in regular way 
	 */
	boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext context) throws IOException;
	
	void triggerAutomatedUIAuthentication(VaadinAuthenticationUI authenticatorUI);
}
