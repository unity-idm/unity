/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.google.common.collect.Sets;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

/**
 * Allows to select a new entity state
 * @author K. Benedyczak
 */
public class ChangeEntityStateDialog extends AbstractDialog
{
	private final EntityWithLabel entity;
	protected Callback callback;
	
	private EnumComboBox<EntityState> entityState;
	private CheckBox scheduleEnable;
	private EnumComboBox<EntityScheduledOperation> entityScheduledChange;
	private DateField changeTime;
	
	public ChangeEntityStateDialog(UnityMessageSource msg, EntityWithLabel entity, Callback callback)
	{
		super(msg, msg.getMessage("ChangeEntityStateDialog.caption"));
		this.entity = entity;
		this.callback = callback;
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("ChangeEntityStateDialog.info", entity));
		entityState = new EnumComboBox<EntityState>(msg.getMessage("ChangeEntityStateDialog.newState"), msg, 
				"EntityState.", EntityState.class, entity.getEntity().getState(),
				Sets.newHashSet(EntityState.onlyLoginPermitted));
		
		final Panel schedulePanel = new SafePanel();
		FormLayout schedLay = new CompactFormLayout();
		scheduleEnable = new CheckBox(msg.getMessage("ChangeEntityStateDialog.enableScheduled"));
		scheduleEnable.setImmediate(true);
		scheduleEnable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				schedulePanel.setEnabled(scheduleEnable.getValue());
			}
		});
		
		schedulePanel.setContent(schedLay);
		EntityInformation initial = entity.getEntity().getEntityInformation();
		EntityScheduledOperation initialOp = initial.getScheduledOperation() != null ? 
				initial.getScheduledOperation() : EntityScheduledOperation.DISABLE;
		entityScheduledChange = new EnumComboBox<EntityScheduledOperation>(
				msg.getMessage("ChangeEntityStateDialog.scheduledOperation"),
				msg, 
				"EntityScheduledOperation.", EntityScheduledOperation.class, 
				initialOp);
		changeTime = new DateField(msg.getMessage("ChangeEntityStateDialog.scheduledChangeTime"));
		changeTime.setResolution(Resolution.SECOND);
		changeTime.setRequired(true);
		if (initial.getScheduledOperation() != null)
		{
			scheduleEnable.setValue(true);
			changeTime.setValue(initial.getScheduledOperationTime());
		} else
		{
			schedulePanel.setEnabled(false);
		}
		
		schedLay.addComponents(entityScheduledChange, changeTime);
		schedLay.setMargin(true);
		
		FormLayout main = new CompactFormLayout();
		main.addComponents(info, entityState, scheduleEnable, schedulePanel);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		EntityInformation newInfo = new EntityInformation(entityState.getSelectedValue());
		changeTime.setComponentError(null);

		if (scheduleEnable.getValue())
		{
			if (!changeTime.isValid())
			{
				changeTime.setComponentError(new UserError(
						msg.getMessage("fieldRequired")));
				return;
			}
			newInfo.setScheduledOperation(entityScheduledChange.getSelectedValue());
			newInfo.setScheduledOperationTime(changeTime.getValue());
		}
		if (callback.onChanged(newInfo))
			close();
	}
	
	public interface Callback 
	{
		public boolean onChanged(EntityInformation newState);
	}
}
