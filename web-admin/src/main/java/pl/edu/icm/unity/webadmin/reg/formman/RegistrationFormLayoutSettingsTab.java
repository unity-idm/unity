/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.function.Supplier;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

/**
 * General registration layouts settings editor.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutSettingsTab extends CustomComponent
{
	private UnityMessageSource msg;
	private Supplier<RegistrationForm> formProvider;
	private CheckBox compactInputs;
	private TextField logo;

	public RegistrationFormLayoutSettingsTab(UnityMessageSource msg, Supplier<RegistrationForm> formProvider)
	{
		this.msg = msg;
		this.formProvider = formProvider;

		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		compactInputs = new CheckBox(msg.getMessage("FormLayoutEditor.compactInputs"));
		logo = new TextField(msg.getMessage("FormLayoutEditor.logo"));
		logo.setDescription(msg.getMessage("FormLayoutEditor.logoDesc"));
		logo.setWidth(100, Unit.PERCENTAGE);
		main.addComponents(compactInputs, logo);
		setCompositionRoot(main);
	}

	public FormLayoutSettings getSettings()
	{
		FormLayoutSettings settings = FormLayoutSettings.builder()
				.withColumnWidth(FormLayoutSettings.DEFAULT.getColumnWidth())
				.withColumnWidthUnit(FormLayoutSettings.DEFAULT.getColumnWidthUnit())
				.withCompactInputs(compactInputs.getValue())
				.withLogo(logo.getValue() != null && !logo.getValue().isEmpty() ? logo.getValue() : null)
				.build(); 
		return settings;
	}

	public void setSettings(FormLayoutSettings settings)
	{
		compactInputs.setValue(settings.isCompactInputs());
		if (settings.getLogoURL() != null)
			logo.setValue(settings.getLogoURL());
	}

	public void updateFromForm()
	{
		RegistrationForm form = formProvider.get();
		if (form == null)
			return;

		setSettings(form.getLayoutSettings());
	}

}
