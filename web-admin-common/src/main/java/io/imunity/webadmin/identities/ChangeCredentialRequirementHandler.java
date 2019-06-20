/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.identities;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which allow for changing entity's credential requirements
 * 
 * @author K. Benedyczak
 */
@Component
public class ChangeCredentialRequirementHandler
{
	@Autowired
	private CredentialRequirementManagement credReqMan;
	@Autowired
	private EntityCredentialManagement eCredMan;
	@Autowired
	private UnityMessageSource msg;
	
	public SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeCredentialRequirementAction"))
				.withIcon(Images.key.getResource())
				.withHandler(selection -> showDialog(selection,
						refreshCallback))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		String currentCredId = entity.getEntity().getCredentialInfo()
				.getCredentialRequirementId();
		new ChangeCredentialRequirementDialog(msg, entity, currentCredId, eCredMan,
				credReqMan, () -> refreshCallback.run()).show();
	}
}
