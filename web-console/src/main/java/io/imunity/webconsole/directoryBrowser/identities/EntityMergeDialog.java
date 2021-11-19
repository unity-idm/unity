/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;

import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Entity merging dialog. Allows for choosing the target and merged entities as well as the safe mode.
 * @author K. Benedyczak
 */
class EntityMergeDialog extends AbstractDialog
{
	private enum Direction
	{
		FIRST_INTO_SECOND,
		SECOND_INTO_FISRT
	}
	private EntityManagement identitiesMan;
	private RadioButtonGroup<Direction> mergeDirection; 
	private CheckBox safeMode;
	private EventsBus bus;
	private EntityWithLabel first;
	private EntityWithLabel second;
	private Group group;
	
	EntityMergeDialog(MessageSource msg, EntityWithLabel first, EntityWithLabel second, Group group, 
			EntityManagement identitiesMan)
	{
		super(msg, msg.getMessage("EntityMergeDialog.caption"), msg.getMessage("EntityMergeDialog.doMerge"),
				msg.getMessage("cancel"));
		this.first = first;
		this.second = second;
		this.group = group;
		this.identitiesMan = identitiesMan;
		this.bus = WebSession.getCurrent().getEventBus();
		setSizeEm(45, 30);
	}

	@Override
	protected FormLayout getContents() throws EngineException
	{
		FormLayout main = new FormLayout();

		Label info = new Label(msg.getMessage("EntityMergeDialog.info"));
		info.setWidth(100, Unit.PERCENTAGE);
		mergeDirection = new RadioButtonGroup<>(msg.getMessage("EntityMergeDialog.mergeDirection"));
		mergeDirection.setItems(Direction.FIRST_INTO_SECOND, Direction.SECOND_INTO_FISRT);
		mergeDirection.setItemCaptionGenerator(value -> value == Direction.FIRST_INTO_SECOND ? 
				msg.getMessage("EntityMergeDialog.mergeSpec",
						getEntityDesc(first), getEntityDesc(second)) :
				msg.getMessage("EntityMergeDialog.mergeSpec",
						getEntityDesc(second), getEntityDesc(first))
				);
		mergeDirection.setValue(Direction.FIRST_INTO_SECOND);
		
		
		safeMode = new CheckBox(msg.getMessage("EntityMergeDialog.safeMode"));
		safeMode.setValue(true);
		safeMode.setDescription(msg.getMessage("EntityMergeDialog.safeModeDesc"));
		
		main.addComponents(info, mergeDirection, safeMode);
		main.setSizeFull();
		return main;
	}

	private String getEntityDesc(EntityWithLabel e)
	{
		return e.getLabel() != null ? e.getLabel() : "[" + e.getEntity().getId() + "]";
	}
	
	@Override
	protected void onConfirm()
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
			NotificationPopup.showError(msg, msg.getMessage("EntityMergeDialog.mergeError"), e);
		}
		close();
	}
}
