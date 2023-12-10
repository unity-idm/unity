/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;


@Component
class EntityMergeHandler
{
	private final EntityManagement identitiesMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	EntityMergeHandler(EntityManagement identitiesMan, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Supplier<Group> groupSupplier)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.mergeEntitiesAction"))
				.withIcon(VaadinIcon.LINK)
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
		return Objects.equals(e1.getEntity().getId(), e2.getEntity().getId());
	}
	
	private void showDialog(Set<IdentityEntry> selection, Supplier<Group> groupSupplier)
	{       
		Iterator<IdentityEntry> iterator = selection.iterator();
		EntityWithLabel e1 = iterator.next().getSourceEntity();
		EntityWithLabel e2 = iterator.next().getSourceEntity();
		EntityMergeDialog dialog = new EntityMergeDialog(msg, e1, e2,
				groupSupplier.get(), identitiesMan, notificationPresenter);
		dialog.open();
	}
}
