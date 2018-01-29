/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Entity merging dialog. Allows for choosing the target and merged entities as well as the safe mode.
 * @author K. Benedyczak
 */
public class EntityMergeDialog extends AbstractDialog
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
	private String group;
	
	public EntityMergeDialog(UnityMessageSource msg, EntityWithLabel first, EntityWithLabel second, String group, 
			EntityManagement identitiesMan)
	{
		super(msg, msg.getMessage("EntitiesMergeDialog.caption"), msg.getMessage("EntitiesMergeDialog.doMerge"),
				msg.getMessage("cancel"));
		this.first = first;
		this.second = second;
		this.group = group;
		this.identitiesMan = identitiesMan;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	protected FormLayout getContents() throws EngineException
	{
		FormLayout main = new FormLayout();

		Label info = new Label(msg.getMessage("EntitiesMergeDialog.info"));
		info.setWidth(100, Unit.PERCENTAGE);
		mergeDirection = new RadioButtonGroup<>(msg.getMessage("EntitiesMergeDialog.mergeDirection"));
		mergeDirection.setItems(Direction.FIRST_INTO_SECOND, Direction.SECOND_INTO_FISRT);
		mergeDirection.setItemCaptionGenerator(value -> value == Direction.FIRST_INTO_SECOND ? 
				msg.getMessage("EntitiesMergeDialog.mergeSpec",
						getEntityDesc(first), getEntityDesc(second)) :
				msg.getMessage("EntitiesMergeDialog.mergeSpec",
						getEntityDesc(second), getEntityDesc(first))
				);
		mergeDirection.setValue(Direction.FIRST_INTO_SECOND);
		
		
		safeMode = new CheckBox(msg.getMessage("EntitiesMergeDialog.safeMode"));
		safeMode.setValue(true);
		safeMode.setDescription(msg.getMessage("EntitiesMergeDialog.safeModeDesc"));
		
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
			NotificationPopup.showError(msg, msg.getMessage("EntitiesMergeDialog.mergeError"), e);
		}
		close();
	}
}
