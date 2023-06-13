/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.authenticators;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Implementations allow to edit a configuration of a authenticator of a fixed type 
 * @author P.Piernik 
 *
 */
public interface AuthenticatorEditor
{
	Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher, boolean forceNameEditable);
	
	AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException;
	
}
