/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.stream.Stream;

import org.springframework.util.StringUtils;

import com.vaadin.data.Binder;
import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToFloatConverter;
import com.vaadin.data.validator.FloatRangeValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotNullComboBox;

/**
 * General registration layouts settings editor.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutSettingsEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private CheckBox compactInputs;
	private TextField logo;
	private TextField columnWidth;
	private ComboBox<String> columnWidthUnit;
	
	private Binder<FormLayoutSettings> binder;
	
	public RegistrationFormLayoutSettingsEditor(UnityMessageSource msg)
	{
		this.msg = msg;

		initUI();
	}

	private void initUI()
	{
		FormLayout main = new FormLayout();
		main.setSpacing(true);
		compactInputs = new CheckBox(msg.getMessage("FormLayoutEditor.compactInputs"));
		logo = new TextField(msg.getMessage("FormLayoutEditor.logo"));
		logo.setDescription(msg.getMessage("FormLayoutEditor.logoDesc"));
		logo.setWidth(100, Unit.PERCENTAGE);
		columnWidth = new TextField(msg.getMessage("FormLayoutEditor.columnWidth"));
		columnWidthUnit = new NotNullComboBox<>(msg.getMessage("FormLayoutEditor.columnWidthUnit"));
		columnWidthUnit.setItems(Stream.of(Unit.values()).map(Unit::toString));
		
		main.addComponents(logo, columnWidth, columnWidthUnit, compactInputs);
		setCompositionRoot(main);
		
		binder = new Binder<>(FormLayoutSettings.class);
		binder.forField(compactInputs).bind("compactInputs");
		binder.forField(logo)
			.withConverter(emptyToNull())
			.withNullRepresentation("")
			.bind("logoURL");
		binder.forField(columnWidth)
			.asRequired(msg.getMessage("fieldRequired"))
			.withConverter(new StringToFloatConverter(msg.getMessage("FormLayoutEditor.columnWidth.mustBeFloat")))
			.withValidator(new FloatRangeValidator(msg.getMessage("FormLayoutEditor.columnWidth.mustBePositive"), 0f, Float.MAX_VALUE))
			.bind("columnWidth");
		binder.forField(columnWidthUnit)
			.asRequired(msg.getMessage("fieldRequired"))
			.bind("columnWidthUnit");
		binder.setBean(FormLayoutSettings.DEFAULT);
	}

	public FormLayoutSettings getSettings() throws FormValidationException
	{
		return binder.getBean();
	}

	public void setSettings(FormLayoutSettings settings)
	{
		binder.setBean(settings);
	}

	private Converter<String, String> emptyToNull()
	{
		return new Converter<String, String>()
		{
			@Override
			public String convertToPresentation(String value, ValueContext context)
			{
				return value;
			}
			
			@Override
			public Result<String> convertToModel(String value, ValueContext context)
			{
				if (StringUtils.isEmpty(value))
					return Result.ok(null);
				return Result.ok(value);
			}
		};
	}
}
