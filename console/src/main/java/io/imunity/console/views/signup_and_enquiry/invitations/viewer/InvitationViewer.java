/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.viewer;


import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;

public interface InvitationViewer 
{
	void setInput(InvitationWithCode invitation) throws IllegalFormTypeException;
	InvitationType getSupportedType();
	Component getComponent();
}
