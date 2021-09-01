/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

public interface InvitationViewer extends Component
{
	void setInput(InvitationWithCode invitation) throws IllegalFormTypeException;
	InvitationType getSupportedType();
}
