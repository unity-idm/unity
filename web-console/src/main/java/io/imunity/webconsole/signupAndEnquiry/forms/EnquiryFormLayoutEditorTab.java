/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import java.util.function.Supplier;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.signupAndEnquiry.forms.layout.FormLayoutEditor;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.FormLayoutUtils;
import pl.edu.icm.unity.base.registration.layout.FormLayout;

/**
 * Enquiry layout editor. Allows for selecting whether the default layout should
 * be used or not.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class EnquiryFormLayoutEditorTab extends CustomComponent
{
	private MessageSource msg;
	private Supplier<EnquiryForm> formProvider;
	private CheckBox enableCustomLayout;
	private FormLayoutEditor layoutEditor;
	private boolean isInitialValueSet = false;

	public EnquiryFormLayoutEditorTab(MessageSource msg, Supplier<EnquiryForm> formProvider)
	{
		super();
		this.msg = msg;
		this.formProvider = formProvider;

		initUI();
	}

	private void initUI()
	{
		layoutEditor = new FormLayoutEditor(msg, () -> formProvider.get().getEffectiveFormLayout(msg));
		layoutEditor.setSizeFull();
		
		enableCustomLayout = new CheckBox(msg.getMessage("FormLayoutEditor.enableCustom"));
		enableCustomLayout.addValueChangeListener(event -> onEnableCustomLayout(event.getValue()));
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(true);
		main.addComponents(enableCustomLayout, layoutEditor);
		setCompositionRoot(main);
	}

	private void onEnableCustomLayout(boolean isCustomLayout)
	{
		layoutEditor.setVisible(isCustomLayout);
		if (isCustomLayout && isInitialValueSet)
		{
			setLayoutFromProvider();
		}
	}
	
	public FormLayout getLayout()
	{
		updateFromForm();
		return getCurrentLayout();
	}
	
	public FormLayout getCurrentLayout()
	{
		if (enableCustomLayout.getValue())
		{
			return layoutEditor.getLayout();
		}
		return null;
	}

	public void setLayout(FormLayout layout)
	{
		layoutEditor.setLayout(layout);
		if (!isInitialValueSet)
		{
			boolean isEnableCustomLayout = layout != null;
			enableCustomLayout.setValue(isEnableCustomLayout);
			onEnableCustomLayout(isEnableCustomLayout);
			isInitialValueSet = true;
		}
	}
	
	private void setLayoutFromProvider()
	{
		if (!enableCustomLayout.getValue())
			return;
		layoutEditor.setLayoutFromProvider();
	}
	
	public void updateFromForm()
	{
		EnquiryForm form = formProvider.get();
		if (form == null)
			return;
		
		FormLayout layout = getCurrentLayout();
		FormLayoutUtils.updateEnquiryLayout(layout, form);
		setLayout(layout);
	}

}
