/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

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
	private DateTimeField changeTime;
	
	public ChangeEntityStateDialog(UnityMessageSource msg, EntityWithLabel entity, Callback callback)
	{
		super(msg, msg.getMessage("ChangeEntityStateDialog.caption"));
		this.entity = entity;
		this.callback = callback;
		setSizeMode(SizeMode.MEDIUM);
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("ChangeEntityStateDialog.info", entity));
		entityState = new EnumComboBox<>(msg.getMessage("ChangeEntityStateDialog.newState"), msg, 
				"EntityState.", EntityState.class, entity.getEntity().getState(),
				t -> t != EntityState.onlyLoginPermitted);
		
		final Panel schedulePanel = new SafePanel();
		FormLayout schedLay = new CompactFormLayout();
		scheduleEnable = new CheckBox(msg.getMessage("ChangeEntityStateDialog.enableScheduled"));
		scheduleEnable.addValueChangeListener(event -> schedulePanel.setEnabled(scheduleEnable.getValue()));
		
		schedulePanel.setContent(schedLay);
		EntityInformation initial = entity.getEntity().getEntityInformation();
		EntityScheduledOperation initialOp = initial.getScheduledOperation() != null ? 
				initial.getScheduledOperation() : EntityScheduledOperation.DISABLE;
		entityScheduledChange = new EnumComboBox<>(
				msg.getMessage("ChangeEntityStateDialog.scheduledOperation"),
				msg, 
				"EntityScheduledOperation.", EntityScheduledOperation.class, 
				initialOp);
		changeTime = new DateTimeField(msg.getMessage("ChangeEntityStateDialog.scheduledChangeTime"));
		changeTime.setResolution(DateTimeResolution.SECOND);
		changeTime.setRequiredIndicatorVisible(true);
		if (initial.getScheduledOperation() != null)
		{
			scheduleEnable.setValue(true);
			Instant scheduledAsInstant = initial.getScheduledOperationTime().toInstant();
			LocalDateTime ldt = LocalDateTime.ofInstant(scheduledAsInstant, ZoneId.systemDefault());
			changeTime.setValue(ldt);
		} else
		{
			schedulePanel.setEnabled(false);
		}
		
		schedLay.addComponents(entityScheduledChange, changeTime);
		schedLay.setMargin(true);
		
		FormLayout main = new CompactFormLayout();
		main.addComponents(info);
		
		if (entity.getEntity().getEntityInformation().getRemovalByUserTime() != null &&
				entity.getEntity().getEntityInformation().getState() == EntityState.onlyLoginPermitted)
		{
			Label infoRemovalByUser = new Label(msg.getMessage("ChangeEntityStateDialog.infoUserScheduledRemoval", 
				entity.getEntity().getEntityInformation().getRemovalByUserTime()));
			main.addComponent(infoRemovalByUser);
		}
		
		main.addComponents(entityState, scheduleEnable, schedulePanel);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		EntityState newState = entityState.getValue() == null ? entity.getEntity().getState() :
			entityState.getValue();
		EntityInformation newInfo = new EntityInformation(entity.getEntity().getId());
		newInfo.setState(newState);
		changeTime.setComponentError(null);

		if (scheduleEnable.getValue())
		{
			if (changeTime.getValue() == null)
			{
				changeTime.setComponentError(new UserError(
						msg.getMessage("fieldRequired")));
				return;
			}
			newInfo.setScheduledOperation(entityScheduledChange.getValue());
			LocalDateTime ldt = changeTime.getValue();
			Date zonedDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			newInfo.setScheduledOperationTime(zonedDate);
		}
		
		if (callback.onChanged(newInfo))
			close();
	}
	
	public interface Callback 
	{
		public boolean onChanged(EntityInformation newState);
	}
}
