/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

public interface AssociationAccountWizardProvider
{
	Component getWizardForConnectIdAtLogin(RemotelyAuthenticatedPrincipal unknownUser, Runnable closeWizard);
}
