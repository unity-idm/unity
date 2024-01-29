/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToFloatConverter;
import com.vaadin.flow.data.validator.FloatRangeValidator;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.endpoint.common.file.FileField;
import io.imunity.vaadin.endpoint.common.file.FileFieldUtils;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * General registration layouts settings editor.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutSettingsEditor extends VerticalLayout
{
	private final MessageSource msg;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final VaadinLogoImageLoader imageAccessService;

	private Binder<FormLayoutSettingsWithLogo> binder;

	public RegistrationFormLayoutSettingsEditor(MessageSource msg, UnityServerConfiguration serverConfig,
			FileStorageService fileStorageService, VaadinLogoImageLoader imageAccessService)
	{
		this.msg = msg;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.imageAccessService = imageAccessService;
		initUI();
	}

	private void initUI()
	{
		setPadding(false);
		FormLayout main = new FormLayout();
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		Checkbox compactInputs = new Checkbox(msg.getMessage("FormLayoutEditor.compactInputs"));

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		logo.setEnabled(true);

		TextField columnWidth = new TextField();
		ComboBox<String> columnWidthUnit = new NotEmptyComboBox<>();
		columnWidthUnit.setItems(Stream.of(Unit.values()).map(Unit::toString).toList());

		main.addFormItem(logo, msg.getMessage("FormLayoutEditor.logo"))
				.add(TooltipFactory.get(msg.getMessage("FormLayoutEditor.logoDesc")));
		main.addFormItem(columnWidth, msg.getMessage("FormLayoutEditor.columnWidth"));
		main.addFormItem(columnWidthUnit, msg.getMessage("FormLayoutEditor.columnWidthUnit"));
		main.addFormItem(compactInputs, "");
		add(main);

		binder = new Binder<>(FormLayoutSettingsWithLogo.class);
		binder.forField(compactInputs)
				.bind(FormLayoutSettingsWithLogo::isCompactInputs, FormLayoutSettingsWithLogo::setCompactInputs);
		logo.configureBinding(binder, "logo");
		binder.forField(columnWidth).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToFloatConverter(
						msg.getMessage("FormLayoutEditor.columnWidth.mustBeFloat")))
				.withValidator(new FloatRangeValidator(
						msg.getMessage("FormLayoutEditor.columnWidth.mustBePositive"), 0f,
						Float.MAX_VALUE))
				.bind(FormLayoutSettingsWithLogo::getColumnWidth, FormLayoutSettingsWithLogo::setColumnWidth);
		binder.forField(columnWidthUnit).asRequired(msg.getMessage("fieldRequired"))
				.bind(FormLayoutSettingsWithLogo::getColumnWidthUnit, FormLayoutSettingsWithLogo::setColumnWidthUnit);
		binder.setBean(new FormLayoutSettingsWithLogo(FormLayoutSettings.DEFAULT, imageAccessService));
	}

	public FormLayoutSettings getSettings(String formName) throws FormValidationException
	{
		if(binder.validate().hasErrors())
			throw new FormValidationException();
		return binder.getBean().toFormLayoutSettings(fileStorageService, formName);
	}

	public void setSettings(FormLayoutSettings settings)
	{
		binder.setBean(new FormLayoutSettingsWithLogo(settings, imageAccessService));
	}

	public static class FormLayoutSettingsWithLogo extends FormLayoutSettings
	{
		private LocalOrRemoteResource logo;

		public FormLayoutSettingsWithLogo(FormLayoutSettings org, VaadinLogoImageLoader imageAccessService)
		{
			this.setColumnWidth(org.getColumnWidth());
			this.setColumnWidthUnit(org.getColumnWidthUnit());
			this.setShowCancel(org.isShowCancel());
			this.setCompactInputs(org.isCompactInputs());
			if (org.getLogoURL() != null)
				this.setLogo(imageAccessService.loadImageFromUri(org.getLogoURL()).orElse(null));
		}

		public FormLayoutSettings toFormLayoutSettings(FileStorageService fileStorageService, String formName)
		{
			FormLayoutSettings settings = new FormLayoutSettings();
			settings.setColumnWidth(this.getColumnWidth());
			settings.setColumnWidthUnit(this.getColumnWidthUnit());
			settings.setShowCancel(this.isShowCancel());
			settings.setCompactInputs(this.isCompactInputs());
			settings.setLogoURL(FileFieldUtils.saveFile(getLogo(), fileStorageService,
					FileStorageService.StandardOwner.FORM.toString(), formName));

			return settings;
		}

		public LocalOrRemoteResource getLogo()
		{
			return logo;
		}

		public void setLogo(LocalOrRemoteResource logo)
		{
			this.logo = logo;
		}
	}

}
