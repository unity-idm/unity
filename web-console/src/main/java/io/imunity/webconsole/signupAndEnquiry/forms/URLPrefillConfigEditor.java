/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;

class URLPrefillConfigEditor extends CustomComponent
{
	private CheckBox enabled;
	private Binder<DataBean> binder;
	
	URLPrefillConfigEditor(MessageSource msg)
	{
		enabled = new CheckBox(msg.getMessage("URLPrefillConfigEditor.enable"));
		TextField paramName = new TextField();
		paramName.setPlaceholder(msg.getMessage("URLPrefillConfigEditor.param"));
		EnumComboBox<PrefilledEntryMode> mode = new EnumComboBox<>(msg, "PrefilledEntryMode.", 
				PrefilledEntryMode.class, PrefilledEntryMode.DEFAULT);
		mode.setWidth(18, Unit.EM);
		HorizontalLayout settingsWrapper = new HorizontalLayout(paramName, mode);
		settingsWrapper.setMargin(new MarginInfo(false, false, true, false));
		settingsWrapper.setVisible(false);
		
		enabled.addValueChangeListener(e -> settingsWrapper.setVisible(e.getValue()));
		
		VerticalLayout main = new VerticalLayout(enabled, settingsWrapper);
		main.addStyleName(Styles.smallSpacing.toString());
		main.setComponentAlignment(enabled, Alignment.TOP_LEFT);
		main.setComponentAlignment(settingsWrapper, Alignment.TOP_LEFT);
		main.setMargin(false);
		setCompositionRoot(main);
		
		binder = new Binder<>(DataBean.class);
		binder.forField(paramName).asRequired(msg.getMessage("fieldRequired")).bind("paramName");
		binder.forField(mode).bind("mode");
		binder.setBean(new DataBean());
	}
	
	URLQueryPrefillConfig getValue() throws FormValidationException
	{
		if (enabled.getValue() == false)
			return null;
		BinderValidationStatus<DataBean> status = binder.validate();
		if (status.hasErrors())
			throw new FormValidationException();
		return binder.getBean().toDomainType();
	}
	
	void setValue(URLQueryPrefillConfig value)
	{
		enabled.setValue(value != null);
		binder.setBean(value == null ? new DataBean() : new DataBean(value));
	}
	
	
	
	public static class DataBean
	{
		private String paramName;
		private PrefilledEntryMode mode = PrefilledEntryMode.DEFAULT;
		
		public DataBean()
		{
		}
		
		public DataBean(URLQueryPrefillConfig config)
		{
			this.paramName = config.paramName;
			this.mode = config.mode;
		}
		
		URLQueryPrefillConfig toDomainType()
		{
			return new URLQueryPrefillConfig(paramName, mode);
		}
		
		public String getParamName()
		{
			return paramName;
		}
		public void setParamName(String paramName)
		{
			this.paramName = paramName;
		}
		public PrefilledEntryMode getMode()
		{
			return mode;
		}
		public void setMode(PrefilledEntryMode mode)
		{
			this.mode = mode;
		}
	}
}
