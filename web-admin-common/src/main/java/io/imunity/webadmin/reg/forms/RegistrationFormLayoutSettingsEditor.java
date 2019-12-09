/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.reg.forms;

import java.util.stream.Stream;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToFloatConverter;
import com.vaadin.data.validator.FloatRangeValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.file.ImageUtils;

/**
 * General registration layouts settings editor.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutSettingsEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;

	private Binder<FormLayoutSettingsWithLogo> binder;

	public RegistrationFormLayoutSettingsEditor(UnityMessageSource msg, UnityServerConfiguration serverConfig,
			FileStorageService fileStorageService, URIAccessService uriAccessService)
	{
		this.msg = msg;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		initUI();
	}

	private void initUI()
	{
		FormLayout main = new FormLayout();
		main.setSpacing(true);
		CheckBox compactInputs = new CheckBox(msg.getMessage("FormLayoutEditor.compactInputs"));

		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("FormLayoutEditor.logo"));
		logo.setDescription(msg.getMessage("FormLayoutEditor.logoDesc"));

		TextField columnWidth = new TextField(msg.getMessage("FormLayoutEditor.columnWidth"));
		ComboBox<String> columnWidthUnit = new NotNullComboBox<>(
				msg.getMessage("FormLayoutEditor.columnWidthUnit"));
		columnWidthUnit.setItems(Stream.of(Unit.values()).map(Unit::toString));

		main.addComponents(logo, columnWidth, columnWidthUnit, compactInputs);
		setCompositionRoot(main);

		binder = new Binder<>(FormLayoutSettingsWithLogo.class);
		binder.forField(compactInputs).bind("compactInputs");
		logo.configureBinding(binder, "logo");
		binder.forField(columnWidth).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToFloatConverter(
						msg.getMessage("FormLayoutEditor.columnWidth.mustBeFloat")))
				.withValidator(new FloatRangeValidator(
						msg.getMessage("FormLayoutEditor.columnWidth.mustBePositive"), 0f,
						Float.MAX_VALUE))
				.bind("columnWidth");
		binder.forField(columnWidthUnit).asRequired(msg.getMessage("fieldRequired")).bind("columnWidthUnit");
		binder.setBean(new FormLayoutSettingsWithLogo(FormLayoutSettings.DEFAULT, uriAccessService));
	}

	public FormLayoutSettings getSettings(String formName) throws FormValidationException
	{
		return binder.getBean().toFormLayoutSettings(fileStorageService, formName);
	}

	public void setSettings(FormLayoutSettings settings)
	{
		binder.setBean(new FormLayoutSettingsWithLogo(settings, uriAccessService));
	}

	public static class FormLayoutSettingsWithLogo extends FormLayoutSettings
	{
		private LocalOrRemoteResource logo;

		public FormLayoutSettingsWithLogo()
		{

		}

		public FormLayoutSettingsWithLogo(FormLayoutSettings org, URIAccessService uriAccessService)
		{
			this.setColumnWidth(org.getColumnWidth());
			this.setColumnWidthUnit(org.getColumnWidthUnit());
			this.setShowCancel(org.isShowCancel());
			this.setCompactInputs(org.isCompactInputs());
			if (org.getLogoURL() != null)
			{
				this.setLogo(ImageUtils.getImageFromUriOrNull(org.getLogoURL(), uriAccessService));
			}
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
