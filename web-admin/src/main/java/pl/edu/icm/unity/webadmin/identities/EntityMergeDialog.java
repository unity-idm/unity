/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;

/**
 * Entity merging dialog. Allows for choosing the target and merged entities as well as the safe mode.
 * @author K. Benedyczak
 */
public class EntityMergeDialog extends AbstractDialog
{
	private static final String FIRST_INTO_SECOND = "fis";
	private static final String SECOND_INTO_FISRT = "sif";
	private IdentitiesManagement identitiesMan;
	private OptionGroup mergeDirection; 
	private CheckBox safeMode;
	private EventsBus bus;
	private EntityWithLabel first;
	private EntityWithLabel second;
	private String group;
	
	public EntityMergeDialog(UnityMessageSource msg, EntityWithLabel first, EntityWithLabel second, String group, 
			IdentitiesManagement identitiesMan)
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
		
		mergeDirection = new OptionGroup(msg.getMessage("EntitiesMergeDialog.mergeDirection"));
		mergeDirection.addItem(FIRST_INTO_SECOND);
		mergeDirection.setItemCaption(FIRST_INTO_SECOND, msg.getMessage("EntitiesMergeDialog.mergeSpec",
				getEntityDesc(first), getEntityDesc(second)));
		mergeDirection.addItem(SECOND_INTO_FISRT);
		mergeDirection.setItemCaption(SECOND_INTO_FISRT, msg.getMessage("EntitiesMergeDialog.mergeSpec",
				getEntityDesc(second), getEntityDesc(first)));
		mergeDirection.setValue(FIRST_INTO_SECOND);
		
		
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
		if (FIRST_INTO_SECOND.equals(mergeDirection.getValue()))
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
