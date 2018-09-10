/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.function.Supplier;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

/**
 * Registration for layouts and settings editor.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private Supplier<RegistrationForm> formProvider;
	//TODO use Binder!
	private CheckBox localSignupEmbeddedAsButton;
	private CheckBox compactInputs;
	private CheckBox showCancel;

	public RegistrationFormLayoutEditor(UnityMessageSource msg, Supplier<RegistrationForm> formProvider)
	{
		this.msg = msg;
		this.formProvider = formProvider;

		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		localSignupEmbeddedAsButton = new CheckBox(msg.getMessage("FormLayoutEditor.localSignupEmbeddedAsButton"));
		compactInputs = new CheckBox(msg.getMessage("FormLayoutEditor.compactInputs"));
		showCancel = new CheckBox(msg.getMessage("FormLayoutEditor.showCancel"));
		main.addComponents(localSignupEmbeddedAsButton, compactInputs, showCancel);
		setCompositionRoot(main);
	}

	public RegistrationFormLayouts getLayouts()
	{
		RegistrationFormLayouts layouts = new RegistrationFormLayouts();
		layouts.setLocalSignupEmbeddedAsButton(localSignupEmbeddedAsButton.getValue());
		return layouts;
	}

	public FormLayoutSettings getSettings()
	{
		FormLayoutSettings settings = FormLayoutSettings.builder()
				.withColumnWidth(FormLayoutSettings.DEFAULT.getColumnWidth())
				.withColumnWidthUnit(FormLayoutSettings.DEFAULT.getColumnWidthUnit())
				.withCompactInputs(compactInputs.getValue())
				.withShowCancel(showCancel.getValue())
				.build(); 
		return settings;
	}

	public void setLayouts(RegistrationFormLayouts layouts)
	{
		localSignupEmbeddedAsButton.setValue(layouts.isLocalSignupEmbeddedAsButton());
	}

	public void setSettings(FormLayoutSettings settings)
	{
		compactInputs.setValue(settings.isCompactInputs());
		showCancel.setValue(settings.isShowCancel());
	}

	public void updateFromForm()
	{
		RegistrationForm form = formProvider.get();
		if (form == null)
			return;

		setSettings(form.getLayoutSettings());
		setLayouts(form.getFormLayouts());
	}

}
