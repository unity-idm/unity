/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.function.Supplier;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.TabSheet;
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
public class RegistrationFormLayoutMainComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private RegistrationFormLayoutSettingsTab settingsTab;
	private RegistrationFormLayoutEditorTab editorTab;

	private Supplier<RegistrationForm> formProvider;
	
	public RegistrationFormLayoutMainComponent(UnityMessageSource msg, Supplier<RegistrationForm> formProvider)
	{
		this.msg = msg;
		this.formProvider = formProvider;
		initUI();
	}

	private void initUI()
	{
		setSizeFull();
		setMargin(false);
		setSpacing(false);
		TabSheet tabs = new TabSheet();
		
		settingsTab = new RegistrationFormLayoutSettingsTab(msg, formProvider);
		tabs.addTab(settingsTab, msg.getMessage("RegistrationFormEditor.layoutSettings"));
		
		editorTab = new RegistrationFormLayoutEditorTab(msg, formProvider);
		tabs.addTab(editorTab, msg.getMessage("RegistrationFormEditor.layoutContent"));
		addComponent(tabs);
		setComponentAlignment(tabs, Alignment.TOP_LEFT);
		setExpandRatio(tabs, 1);
	}

	public void updateFromForm()
	{
		settingsTab.updateFromForm();
		editorTab.updateFromForm();
	}

	public RegistrationFormLayouts getLayouts()
	{
		return editorTab.getLayouts();
	}

	public FormLayoutSettings getSettings()
	{
		return settingsTab.getSettings();
	}

	public void setSettings(FormLayoutSettings layoutSettings)
	{
		settingsTab.setSettings(layoutSettings);
	}

	public void setInitialLayouts(RegistrationFormLayouts formLayouts)
	{
		editorTab.setFormLayouts(formLayouts);
	}

}
