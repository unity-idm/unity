/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8.PostAuthenticationStepDecision;

/**
 * Component providing a complete authentication screen. This component will be used to 
 * fill the {@link AuthenticationUI} 
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationScreen extends Component 
{
	/**
	 * After handling this method the authN UI should be reset to initial state - ready for next login
	 */
	void reset();
	
	void initializeAfterReturnFromExternalAuthn(PostAuthenticationStepDecision postAuthnStepDecision);
}
