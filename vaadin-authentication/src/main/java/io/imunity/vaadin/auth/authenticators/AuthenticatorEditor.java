/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.authenticators;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


public interface AuthenticatorEditor
{
	Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher, boolean forceNameEditable);
	
	AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException;
	
}
