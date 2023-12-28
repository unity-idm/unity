/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import com.vaadin.flow.component.Component;

import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.webui.common.FormValidationException;

interface InvitationParamEditor
{
	InvitationParam getInvitation() throws FormValidationException;
	Component getComponent();
}
