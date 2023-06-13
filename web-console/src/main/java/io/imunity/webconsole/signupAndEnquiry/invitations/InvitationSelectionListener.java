/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;

/**
 * 
 * @author P.Piernik
 *
 */
public interface InvitationSelectionListener
{
	void invitationChanged(InvitationWithCode invitation);
}