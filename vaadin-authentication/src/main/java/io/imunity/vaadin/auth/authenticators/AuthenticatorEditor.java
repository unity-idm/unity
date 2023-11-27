/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.authenticators;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;


public interface AuthenticatorEditor
{
	Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher, boolean forceNameEditable);
	
	AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException;
	
}
