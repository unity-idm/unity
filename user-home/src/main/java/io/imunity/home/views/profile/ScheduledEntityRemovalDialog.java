/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Date;
import java.util.Map;

;

/**
 * Dialog allowing to either immediately or with grace period schedule the entity removal.
 */
class ScheduledEntityRemovalDialog extends Dialog
{
	private final static String NOW = "now";
	private final static String SCHEDULE = "sched";
			
	private final long entity;
	private final MessageSource msg;
	private final VaddinWebLogoutHandler authnProcessor;
	private final EntityManagement identitiesMan;
	private final NotificationPresenter notificationPresenter;

	private RadioButtonGroup<String> nowOrLater;
	private Integer days = 0;
	private Binder<Integer> daysBinder;
	
	ScheduledEntityRemovalDialog(MessageSource msg, long entityId,
			EntityManagement identitiesManagement, VaddinWebLogoutHandler authnProcessor, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.entity = entityId;
		this.identitiesMan = identitiesManagement;
		this.authnProcessor = authnProcessor;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(msg.getMessage("RemoveEntityDialog.caption"));
		add(getContents());
		getFooter().add(new Button(msg.getMessage("cancel"), e -> close()), new Button(msg.getMessage("ok"), e -> onConfirm()));
	}

	protected Component getContents()
	{
		nowOrLater = new RadioButtonGroup<>();
		Map<String, String> captions = Map.of(NOW,
				msg.getMessage("RemoveEntityDialog.removeNow"), SCHEDULE,
				msg.getMessage("RemoveEntityDialog.scheduleRemoval"));
		nowOrLater.setItems(captions.keySet());
		nowOrLater.setItemLabelGenerator(captions::get);
		nowOrLater.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		TextField daysField = new TextField();
		daysBinder = new Binder<>();
		daysBinder.forField(daysField).withConverter(new StringToIntegerConverter(msg.getMessage("RemoveEntityDialog.notANumber")))
		.withValidator(new IntegerRangeValidator(msg.getMessage("RemoveEntityDialog.notANumber",
				365), 1, 365)).bind(s -> this.days, (s, v) -> this.days = v);
		daysBinder.setBean(days);
		daysField.setValue("30");
		
		Label daysL = new Label(msg.getMessage("RemoveEntityDialog.days"));
		HorizontalLayout daysHL = new HorizontalLayout(daysField, daysL);
		daysHL.setAlignItems(FlexComponent.Alignment.CENTER);
		daysHL.setMargin(false);

		Label schedInfo = new Label(msg.getMessage("RemoveEntityDialog.scheduleInfo"));
		nowOrLater.addValueChangeListener(e -> {
			boolean enableSched = SCHEDULE.equals(nowOrLater.getValue());
			daysHL.setEnabled(enableSched);
			schedInfo.setEnabled(enableSched);
		});
		
		
		nowOrLater.setValue(SCHEDULE);
		
		VerticalLayout main = new VerticalLayout(nowOrLater, daysHL, schedInfo);
		main.setMargin(false);
		return main;
	}

	protected void onConfirm()
	{
		boolean now = NOW.equals(nowOrLater.getValue());
		final Date time;
		String confirmQuestion; 
		if (now)
		{
			time = new Date();
			confirmQuestion = msg.getMessage("RemoveEntityDialog.confirmImmediate");
		} else
		{
			if (!daysBinder.isValid())
			{
				daysBinder.validate();
				return;
			}
			
			time = new Date(System.currentTimeMillis() + 1000L * 3600 * 24 * this.days);
			confirmQuestion = msg.getMessage("RemoveEntityDialog.confirmScheduled", time);
		}
		
		ConfirmDialog confirm = new ConfirmDialog();
		confirm.setText(confirmQuestion);
		confirm.setHeader(msg.getMessage("ConfirmDialog.confirm"));
		confirm.addConfirmListener(e -> scheduleChange(time));
		confirm.setCancelButton(msg.getMessage("cancel"), e -> close());
		confirm.open();
	}

	private void scheduleChange(Date time)
	{
		try
		{
			identitiesMan.scheduleRemovalByUser(new EntityParam(entity), time);
			close();
			authnProcessor.logout();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("RemoveEntityDialog.scheduleFailed"), e.getMessage());
		}
	}
}
