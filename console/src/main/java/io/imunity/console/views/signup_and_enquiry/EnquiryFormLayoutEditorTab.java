/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.signup_and_enquiry.layout.FormLayoutEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.FormLayoutUtils;
import pl.edu.icm.unity.base.registration.layout.FormLayout;

import java.util.function.Supplier;


public class EnquiryFormLayoutEditorTab extends VerticalLayout
{
	private final MessageSource msg;
	private final Supplier<EnquiryForm> formProvider;
	private final NotificationPresenter notificationPresenter;
	private Checkbox enableCustomLayout;
	private FormLayoutEditor layoutEditor;
	private boolean isInitialValueSet = false;

	public EnquiryFormLayoutEditorTab(MessageSource msg, Supplier<EnquiryForm> formProvider, NotificationPresenter notificationPresenter)
	{
		super();
		this.msg = msg;
		this.formProvider = formProvider;
		this.notificationPresenter = notificationPresenter;

		initUI();
	}

	private void initUI()
	{
		layoutEditor = new FormLayoutEditor(msg, () ->
		{
			EnquiryForm enquiryForm = formProvider.get();
			if(enquiryForm == null)
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
				return null;
			}
			return enquiryForm.getEffectiveFormLayout(msg);
		});
		layoutEditor.setSizeFull();
		
		enableCustomLayout = new Checkbox(msg.getMessage("FormLayoutEditor.enableCustom"));
		enableCustomLayout.addValueChangeListener(event -> onEnableCustomLayout(event.getValue()));
		
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(enableCustomLayout, layoutEditor);
		setPadding(false);
		add(main);
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
