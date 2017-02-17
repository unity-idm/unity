/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Date;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
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
	private WebAuthenticationProcessor authnProcessor;
	private IdentitiesManagement identitiesMan;
	private OptionGroup nowOrLater;
	private TextField days;
	
	public ScheduledEntityRemovalDialog(UnityMessageSource msg, long entityId, 
			IdentitiesManagement identitiesManagement, WebAuthenticationProcessor authnProcessor)
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
		nowOrLater = new OptionGroup();
		nowOrLater.addItem(NOW);
		nowOrLater.addItem(SCHEDULE);
		nowOrLater.setItemCaption(NOW, msg.getMessage("RemoveEntityDialog.removeNow"));
		nowOrLater.setItemCaption(SCHEDULE, msg.getMessage("RemoveEntityDialog.scheduleRemoval"));
		nowOrLater.setImmediate(true);
		
		days = new TextField();
		days.addValidator(new IntegerRangeValidator(msg.getMessage("RemoveEntityDialog.notANumber", 
				365), 1, 365));
		days.setConverter(new StringToIntegerConverter());
		days.setConversionError(msg.getMessage("RemoveEntityDialog.notANumber"));
		days.setValue("30");
		days.setColumns(4);
		
		Label daysL = new Label(msg.getMessage("RemoveEntityDialog.days"));
		final HorizontalLayout daysHL = new HorizontalLayout(HtmlTag.hspaceEm(1), days, daysL);
		daysHL.setSpacing(true);
		daysHL.setComponentAlignment(daysL, Alignment.BOTTOM_LEFT);
		
		final Label schedInfo = new Label(msg.getMessage("RemoveEntityDialog.scheduleInfo"));
		schedInfo.addStyleName(Styles.vLabelSmall.toString());

		nowOrLater.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean enableSched = SCHEDULE.equals(nowOrLater.getValue());
				daysHL.setEnabled(enableSched);
				schedInfo.setEnabled(enableSched);
			}
		});
		nowOrLater.select(SCHEDULE);
		
		VerticalLayout main = new VerticalLayout(nowOrLater, daysHL, schedInfo);
		main.setSpacing(true);
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
			if (!days.isValid())
				return;
			Integer daysI = (Integer) days.getConvertedValue();
			time = new Date(System.currentTimeMillis() + 1000L * 3600 * 24 * daysI);
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
