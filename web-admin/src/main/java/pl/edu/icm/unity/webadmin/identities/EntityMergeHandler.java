/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
	private UnityMessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction(Supplier<String> groupSupplier)
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
	
	private void showDialog(Set<IdentityEntry> selection, Supplier<String> groupSupplier)
	{       
		Iterator<IdentityEntry> iterator = selection.iterator();
		EntityWithLabel e1 = iterator.next().getSourceEntity();
		EntityWithLabel e2 = iterator.next().getSourceEntity();
		EntityMergeDialog dialog = new EntityMergeDialog(msg, e1, e2, 
				groupSupplier.get(), identitiesMan);
		dialog.show();
	}
}
