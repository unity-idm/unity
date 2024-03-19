/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.message.MessageSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;


class ChangeEntityStateDialog extends DialogWithActionFooter
{
	private static final Locale EUROPEAN_TIME_FORMAT = new Locale("DE");

	private final MessageSource msg;
	private final EntityWithLabel entity;
	private final Callback callback;
	
	private Select<EntityState> entityState;
	private Checkbox scheduleEnable;
	private Select<EntityScheduledOperation> entityScheduledChange;
	private DateTimePicker changeTime;
	private FormLayout.FormItem changeTimeFormItem;

	ChangeEntityStateDialog(MessageSource msg, EntityWithLabel entity, Callback callback)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.entity = entity;
		this.callback = callback;
		setHeaderTitle(msg.getMessage("ChangeEntityStateDialog.caption"));
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		setWidth("50em");
		add(getContents());
	}

	private FormLayout getContents()
	{
		entityState = new Select<>();
		entityState.setLabel(msg.getMessage("ChangeEntityStateDialog.info", entity));
		entityState.setItems(EntityState.values());
		entityState.setItemLabelGenerator(item -> msg.getMessage("EntityState." + item));
		entityState.setValue(entity.getEntity().getState());
		entityState.setWidthFull();

		scheduleEnable = new Checkbox(msg.getMessage("ChangeEntityStateDialog.enableScheduled"));
		
		EntityInformation initial = entity.getEntity().getEntityInformation();
		EntityScheduledOperation initialOp = initial.getScheduledOperation() != null ? 
				initial.getScheduledOperation() : EntityScheduledOperation.DISABLE;
		entityScheduledChange = new Select<>();
		entityScheduledChange.setItems(EntityScheduledOperation.values());
		entityScheduledChange.setItemLabelGenerator(item -> msg.getMessage("EntityScheduledOperation." + item));
		entityScheduledChange.setValue(initialOp);

		changeTime = new DateTimePicker();
		changeTime.setLocale(EUROPEAN_TIME_FORMAT);
		changeTime.setRequiredIndicatorVisible(true);
		if (initial.getScheduledOperation() != null)
		{
			scheduleEnable.setValue(true);
			Instant scheduledAsInstant = initial.getScheduledOperationTime().toInstant();
			LocalDateTime ldt = LocalDateTime.ofInstant(scheduledAsInstant, ZoneId.systemDefault());
			changeTime.setValue(ldt);
		} else
		{
			entityScheduledChange.setEnabled(false);
			changeTime.setEnabled(false);
		}

		scheduleEnable.addValueChangeListener(event ->
		{
			entityScheduledChange.setEnabled(scheduleEnable.getValue());
			changeTime.setEnabled(scheduleEnable.getValue());
		});

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		FormLayout embedded = new FormLayout();
		embedded.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		embedded.addFormItem(entityScheduledChange, msg.getMessage("ChangeEntityStateDialog.scheduledOperation"));
		changeTimeFormItem = embedded.addFormItem(changeTime,
				msg.getMessage("ChangeEntityStateDialog.scheduledChangeTime"));

		if (entity.getEntity().getEntityInformation().getRemovalByUserTime() != null &&
				entity.getEntity().getEntityInformation().getState() == EntityState.onlyLoginPermitted)
		{
			Span infoRemovalByUser = new Span(msg.getMessage("ChangeEntityStateDialog.infoUserScheduledRemoval",
				entity.getEntity().getEntityInformation().getRemovalByUserTime()));
			main.add(infoRemovalByUser);
		}
		
		main.addFormItem(entityState, msg.getMessage("ChangeEntityStateDialog.newState"));
		main.addFormItem(scheduleEnable, "");
		main.addFormItem(embedded, "");
		main.setSizeFull();
		return main;
	}

	private void onConfirm()
	{
		EntityState newState = entityState.getValue() == null ? entity.getEntity().getState() :
			entityState.getValue();
		EntityInformation newInfo = new EntityInformation(entity.getEntity().getId());
		newInfo.setState(newState);
		changeTime.setErrorMessage(null);
		changeTime.setInvalid(false);

		if (scheduleEnable.getValue())
		{
			if (changeTime.getValue() == null)
			{
				changeTime.setErrorMessage(msg.getMessage("fieldRequired"));
				changeTime.setInvalid(true);
				open();
				changeTimeFormItem.getElement().setAttribute("invalid", true);
				return;
			}
			newInfo.setScheduledOperation(entityScheduledChange.getValue());
			LocalDateTime ldt = changeTime.getValue();
			Date zonedDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			newInfo.setScheduledOperationTime(zonedDate);
		}
		
		if (!callback.onChanged(newInfo))
			open();
	}
	
	interface Callback 
	{
		boolean onChanged(EntityInformation newState);
	}
}
