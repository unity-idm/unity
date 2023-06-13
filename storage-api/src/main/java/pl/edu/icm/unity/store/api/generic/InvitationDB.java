/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;


/**
 * Easy access to {@link InvitationWithCode} storage.
 * 
 * @author K. Benedyczak
 */
public interface InvitationDB extends NamedCRUDDAOWithTS<InvitationWithCode>
{
}
