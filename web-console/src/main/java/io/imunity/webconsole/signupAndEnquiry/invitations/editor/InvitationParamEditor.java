/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.webui.common.FormValidationException;

interface InvitationParamEditor
{
	InvitationParam getInvitation() throws FormValidationException;
}
