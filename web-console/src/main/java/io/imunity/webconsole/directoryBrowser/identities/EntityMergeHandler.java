/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which allow for merging 2 entities
 * 
 * @author K. Benedyczak
 */
@Component
class EntityMergeHandler
{
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private MessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction(Supplier<Group> groupSupplier)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.mergeEntitiesAction"))
				.withIcon(Images.transfer.getResource())
				.multiTarget()
				.withDisabledCompositePredicate(this::filter)
				.withHandler(selection -> showDialog(selection,
						groupSupplier))
				.build();
	}

	private boolean filter(Set<IdentityEntry> selection)
	{
		if (selection.size() != 2)
			return true;
		Iterator<IdentityEntry> iterator = selection.iterator();
		EntityWithLabel e1 = iterator.next().getSourceEntity();
		EntityWithLabel e2 = iterator.next().getSourceEntity();
		if (e1.getEntity().getId() == e2.getEntity().getId())
			return true;
		return false;
	}
	
	private void showDialog(Set<IdentityEntry> selection, Supplier<Group> groupSupplier)
	{       
		Iterator<IdentityEntry> iterator = selection.iterator();
		EntityWithLabel e1 = iterator.next().getSourceEntity();
		EntityWithLabel e2 = iterator.next().getSourceEntity();
		EntityMergeDialog dialog = new EntityMergeDialog(msg, e1, e2, 
				groupSupplier.get(), identitiesMan);
		dialog.show();
	}
}
