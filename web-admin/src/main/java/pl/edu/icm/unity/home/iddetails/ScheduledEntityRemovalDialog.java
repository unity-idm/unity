/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Dialog allowing to either immediately or with grace period schedule the entity removal.
 * @author K. Benedyczak
 */
public class ScheduledEntityRemovalDialog extends AbstractDialog
{
	private final static String NOW = "now";
	private final static String SCHEDULE = "sched";
			
	private long entity;
	private StandardWebAuthenticationProcessor authnProcessor;
	private EntityManagement identitiesMan;
	private RadioButtonGroup<String> nowOrLater;
	private TextField daysField;
	private Integer days = 0;
	private Binder<Integer> daysBinder;
	
	public ScheduledEntityRemovalDialog(UnityMessageSource msg, long entityId, 
			EntityManagement identitiesManagement, StandardWebAuthenticationProcessor authnProcessor)
	{
		super(msg, msg.getMessage("RemoveEntityDialog.caption"));
		this.entity = entityId;
		this.identitiesMan = identitiesManagement;
		this.authnProcessor = authnProcessor;
		setSizeMode(SizeMode.MEDIUM);
	}

	@Override
	protected Component getContents() throws Exception
	{
		nowOrLater = new RadioButtonGroup<>();
		Map<String, String> captions = ImmutableMap.of(NOW,
				msg.getMessage("RemoveEntityDialog.removeNow"), SCHEDULE,
				msg.getMessage("RemoveEntityDialog.scheduleRemoval"));
		nowOrLater.setItems(captions.keySet());
		nowOrLater.setItemCaptionGenerator(i -> captions.get(i));
			
		daysField = new TextField();	
		daysBinder = new Binder<Integer>();
		daysBinder.forField(daysField).withConverter(new StringToIntegerConverter(msg.getMessage("RemoveEntityDialog.notANumber")))
		.withValidator(new IntegerRangeValidator(msg.getMessage("RemoveEntityDialog.notANumber", 
				365), 1, 365)).bind(s -> this.days, (s, v) -> this.days = v);
		daysBinder.setBean(days);
		daysField.setValue("30");
		
		Label daysL = new Label(msg.getMessage("RemoveEntityDialog.days"));
		final HorizontalLayout daysHL = new HorizontalLayout(HtmlTag.hspaceEm(1), daysField, daysL);
		daysHL.setMargin(false);
		daysHL.setComponentAlignment(daysL, Alignment.BOTTOM_LEFT);
	
		final Label schedInfo = new Label(msg.getMessage("RemoveEntityDialog.scheduleInfo"));
		schedInfo.addStyleName(Styles.vLabelSmall.toString());	
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

	@Override
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
		
		ConfirmDialog confirm = new ConfirmDialog(msg, confirmQuestion, new Callback()
		{
			@Override
			public void onConfirm()
			{
				scheduleChange(time);
			}
		});
		confirm.show();
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
			NotificationPopup.showError(msg, msg.getMessage("RemoveEntityDialog.scheduleFailed"), e);
		}
	}
}
