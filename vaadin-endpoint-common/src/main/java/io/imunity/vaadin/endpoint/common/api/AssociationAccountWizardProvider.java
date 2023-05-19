/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

public interface AssociationAccountWizardProvider
{
	Component getWizardForConnectId(Runnable finishTask, Runnable closeWizard);
	Component getWizardForConnectIdAtLogin(RemotelyAuthenticatedPrincipal unknownUser, Runnable closeWizard);
}
