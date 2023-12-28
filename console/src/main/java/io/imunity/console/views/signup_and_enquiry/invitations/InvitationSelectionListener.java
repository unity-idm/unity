/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;

/**
 * 
 * @author P.Piernik
 *
 */
interface InvitationSelectionListener
{
	void invitationChanged(InvitationWithCode invitation);
}