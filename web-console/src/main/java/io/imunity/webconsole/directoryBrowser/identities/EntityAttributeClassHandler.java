/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Factory of actions which allow for changing entity's attribute classes
 * 
 * @author K. Benedyczak
 */
@Component("EntityAttributeClassHandlerV8")
class EntityAttributeClassHandler
{
	@Autowired
	private AttributeClassManagement acMan; 
	@Autowired
	private GroupsManagement groupsMan;
	@Autowired
	private MessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback, 
			Supplier<String> groupSupplier)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.editEntityACs"))
				.withIcon(Images.attributes.getResource())
				.withHandler(selection -> showDialog(selection,
						refreshCallback, groupSupplier))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback, 
			Supplier<String> groupSupplier)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		EntityAttributesClassesDialog dialog = new EntityAttributesClassesDialog(
				msg, groupSupplier.get(), entity, acMan, groupsMan, refreshCallback);
		dialog.show();
	}
}
