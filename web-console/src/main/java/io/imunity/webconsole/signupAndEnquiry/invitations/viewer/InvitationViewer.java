/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.viewer;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;

public interface InvitationViewer extends Component
{
	void setInput(InvitationWithCode invitation) throws IllegalFormTypeException;
	InvitationType getSupportedType();
}
