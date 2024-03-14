/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

class URLPrefillConfigEditor extends CustomField<URLQueryPrefillConfig> implements HasValidator<URLQueryPrefillConfig>
{
	private final Checkbox enabled;
	private final Binder<DataBean> binder;
	
	URLPrefillConfigEditor(MessageSource msg)
	{
		enabled = new Checkbox(msg.getMessage("URLPrefillConfigEditor.enable"));
		TextField paramName = new TextField();
		paramName.setPlaceholder(msg.getMessage("URLPrefillConfigEditor.param"));
		Select<PrefilledEntryMode> mode = new Select<>();
		mode.setItemLabelGenerator(item -> msg.getMessage("PrefilledEntryMode." + item));
		mode.setItems(PrefilledEntryMode.values());
		mode.setValue(PrefilledEntryMode.DEFAULT);
		mode.setWidth(TEXT_FIELD_MEDIUM.value());
		HorizontalLayout settingsWrapper = new HorizontalLayout(paramName, mode);
		settingsWrapper.setVisible(false);
		
		enabled.addValueChangeListener(e -> settingsWrapper.setVisible(e.getValue()));
		
		VerticalLayout main = new VerticalLayout(enabled, settingsWrapper);
		main.setPadding(false);
		add(main);
		
		binder = new Binder<>(DataBean.class);
		binder.forField(paramName)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(DataBean::getParamName, DataBean::setParamName);
		binder.forField(mode)
				.bind(DataBean::getMode, DataBean::setMode);
		binder.setBean(new DataBean());
	}

	public URLQueryPrefillConfig getValue()
	{
		if(!enabled.getValue())
			return null;
		URLQueryPrefillConfig domainType = binder.getBean().toDomainType();
		if(domainType.paramName == null)
			return null;
		return domainType;
	}
	
	public void setValue(URLQueryPrefillConfig value)
	{
		enabled.setValue(value != null);
		if(value != null)
			binder.setBean(new DataBean(value));
	}

	@Override
	protected URLQueryPrefillConfig generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(URLQueryPrefillConfig urlQueryPrefillConfig)
	{
		setValue(urlQueryPrefillConfig);
	}

	public void valid() throws FormValidationException
	{
		if (enabled.getValue() && binder.validate().hasErrors())
			throw new FormValidationException();
	}

	@Override
	public Validator<URLQueryPrefillConfig> getDefaultValidator()
	{
		return (value, context) ->
		{
			if (binder.isValid())
				return ValidationResult.ok();
			else
				return ValidationResult.error("");
		};
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
