/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import io.imunity.vaadin.elements.wizard.Wizard;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

public interface AssociationAccountWizardProvider
{
	Wizard getWizardForConnectId(Runnable finishTask);
	Wizard getWizardForConnectIdAtLogin(RemotelyAuthenticatedPrincipal unknownUser);
}
