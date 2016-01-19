/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.db.generic.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Easy access to {@link InvitationWithCode} storage.
 * @author Krzysztof Benedyczak
 */
@Component
public class InvitationWithCodeDB extends GenericObjectsDB<InvitationWithCode>
{
	@Autowired
	public InvitationWithCodeDB(InvitationWithCodeHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, InvitationWithCode.class, "invitation with code");
	}
}
