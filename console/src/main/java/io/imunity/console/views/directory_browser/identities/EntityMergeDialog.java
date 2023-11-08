/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

class EntityMergeDialog extends ConfirmDialog
{
	private enum Direction
	{
		FIRST_INTO_SECOND,
		SECOND_INTO_FISRT
	}
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final EntityManagement identitiesMan;
	private final EventsBus bus;
	private final EntityWithLabel first;
	private final EntityWithLabel second;
	private final Group group;
	private RadioButtonGroup<Direction> mergeDirection;
	private Checkbox safeMode;

	EntityMergeDialog(MessageSource msg, EntityWithLabel first, EntityWithLabel second, Group group, 
			EntityManagement identitiesMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.first = first;
		this.second = second;
		this.group = group;
		this.identitiesMan = identitiesMan;
		this.bus = WebSession.getCurrent().getEventBus();
		setWidth("40em");
		setHeight("25em");
		setHeader(msg.getMessage("EntityMergeDialog.caption"));
		setCancelable(true);
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		add(getContents());
	}

	private FormLayout getContents()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Span info = new Span(msg.getMessage("EntityMergeDialog.info"));
		mergeDirection = new RadioButtonGroup<>();
		mergeDirection.setItems(Direction.FIRST_INTO_SECOND, Direction.SECOND_INTO_FISRT);
		mergeDirection.setItemLabelGenerator(value -> value == Direction.FIRST_INTO_SECOND ?
				msg.getMessage("EntityMergeDialog.mergeSpec",
						getEntityDesc(first), getEntityDesc(second)) :
				msg.getMessage("EntityMergeDialog.mergeSpec",
						getEntityDesc(second), getEntityDesc(first))
				);
		mergeDirection.setValue(Direction.FIRST_INTO_SECOND);

		safeMode = new Checkbox(msg.getMessage("EntityMergeDialog.safeMode"));
		safeMode.setValue(true);
		safeMode.setTooltipText(msg.getMessage("EntityMergeDialog.safeModeDesc"));
		
		main.addFormItem(info, "");
		main.addFormItem(mergeDirection, msg.getMessage("EntityMergeDialog.mergeDirection"));
		main.addFormItem(safeMode, "");
		main.setSizeFull();
		return main;
	}

	private String getEntityDesc(EntityWithLabel e)
	{
		return e.getLabel() != null ? e.getLabel() : "[" + e.getEntity().getId() + "]";
	}

	private void onConfirm()
	{
		if (Direction.FIRST_INTO_SECOND.equals(mergeDirection.getValue()))
		{
			doMerge(second.getEntity(), first.getEntity());
		} else
		{
			doMerge(first.getEntity(), second.getEntity());
		}
	}
	
	private void doMerge(Entity target, Entity merged)
	{
		try
		{
			identitiesMan.mergeEntities(new EntityParam(target.getId()), new EntityParam(merged.getId()), 
					safeMode.getValue());
			bus.fireEvent(new GroupChangedEvent(group));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("EntityMergeDialog.mergeError"), e.getMessage());
		}
		close();
	}
}
