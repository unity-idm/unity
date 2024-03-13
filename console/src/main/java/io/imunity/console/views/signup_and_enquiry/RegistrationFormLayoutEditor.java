/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.signup_and_enquiry.layout.FormLayoutEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.FormLayoutUtils;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormLayouts;

import java.util.function.Supplier;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;

public class RegistrationFormLayoutEditor extends VerticalLayout
{
	private final MessageSource msg;
	private final Supplier<RegistrationForm> formProvider;
	private final NotificationPresenter notificationPresenter;
	private Checkbox enableCustomLayout;
	private FormLayoutEditor primaryLayoutEditor;
	private FormLayoutEditor secondaryLayoutEditor;
	private FormLayout layouts;
	private boolean isInitialValueSet = false;

	public RegistrationFormLayoutEditor(MessageSource msg, Supplier<RegistrationForm> formProvider, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.formProvider = formProvider;
		this.notificationPresenter = notificationPresenter;

		initUI();
	}

	private void initUI()
	{
		layouts = new FormLayout();
		layouts.setResponsiveSteps(
				new FormLayout.ResponsiveStep("0", 1),
				new FormLayout.ResponsiveStep("75em", 2));
		primaryLayoutEditor = new FormLayoutEditor(msg, () ->
		{
			RegistrationForm registrationForm = formProvider.get();
			if(registrationForm == null)
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
				return null;
			}
			return registrationForm.getEffectivePrimaryFormLayout(msg);
		});
		primaryLayoutEditor.setWidth(37, Unit.EM);
		secondaryLayoutEditor = new FormLayoutEditor(msg, () ->
		{
			RegistrationForm registrationForm = formProvider.get();
			if(registrationForm == null)
				return null;
			return registrationForm.getEffectiveSecondaryFormLayout(msg);
		});
		secondaryLayoutEditor.setWidth(37, Unit.EM);
		Panel primaryLayoutPanel = new Panel(msg.getMessage("RegistrationFormEditor.primaryLayout"));
		primaryLayoutPanel.add(primaryLayoutEditor);
		primaryLayoutPanel.setSizeUndefined();
		primaryLayoutPanel.setMargin(false);
		Panel secondaryLayoutPanel = new Panel(msg.getMessage("RegistrationFormEditor.secondaryLayout"));
		secondaryLayoutPanel.add(secondaryLayoutEditor);
		secondaryLayoutPanel.setSizeUndefined();
		secondaryLayoutPanel.setMargin(false);
		secondaryLayoutPanel.getStyle().set("margin-top", BASE_MARGIN.value());

		layouts.add(primaryLayoutPanel, secondaryLayoutPanel);
		layouts.setVisible(false);

		enableCustomLayout = new Checkbox(msg.getMessage("FormLayoutEditor.enableCustom"));
		enableCustomLayout.addValueChangeListener(event -> onEnableCustomLayout(event.getValue()));
		
		setPadding(false);
		add(enableCustomLayout, layouts);
	}
	
	private void onEnableCustomLayout(boolean isCustomLayout)
	{
		layouts.setVisible(isCustomLayout);
		if (isCustomLayout && isInitialValueSet)
			setLayoutFromProvider();
	}
	
	public RegistrationFormLayouts getLayouts()
	{
		updateFromForm();
		return getCurrentLayouts();
	}
	
	public RegistrationFormLayouts getCurrentLayouts()
	{
		RegistrationFormLayouts layouts = new RegistrationFormLayouts();
		if (enableCustomLayout.getValue())
		{
			layouts.setPrimaryLayout(primaryLayoutEditor.getLayout());
			layouts.setSecondaryLayout(secondaryLayoutEditor.getLayout());
		} else
		{
			layouts.setPrimaryLayout(null);
			layouts.setSecondaryLayout(null);
		}
		return layouts;
	}

	public void setFormLayouts(RegistrationFormLayouts formLayouts)
	{
		boolean isCustomLayoutDisabled = formLayouts.getPrimaryLayout() == null 
				&& formLayouts.getSecondaryLayout() == null;
		if (!isInitialValueSet)
		{
			enableCustomLayout.setValue(!isCustomLayoutDisabled);
			onEnableCustomLayout(!isCustomLayoutDisabled);
			isInitialValueSet = true;
		}
		
		if (isCustomLayoutDisabled)
		{
			primaryLayoutEditor.setLayout(null);
			secondaryLayoutEditor.setLayout(null);
		}
		
		if (!enableCustomLayout.getValue())
			return;

		if (!isCustomLayoutDisabled)
		{
			primaryLayoutEditor.setLayout(formLayouts.getPrimaryLayout());
			secondaryLayoutEditor.setLayout(formLayouts.getSecondaryLayout());
		}
	}
	
	private void setLayoutFromProvider()
	{
		if (!enableCustomLayout.getValue())
			return;
		
		primaryLayoutEditor.setLayoutFromProvider();
		secondaryLayoutEditor.setLayoutFromProvider();
		
		
		RegistrationForm form = formProvider.get();
		if (form == null)
			return;
	}
	
	public void updateFromForm()
	{
		RegistrationForm form = formProvider.get();
		if (form == null)
			return;
		
		RegistrationFormLayouts layouts = getCurrentLayouts();
		layouts.setLocalSignupEmbeddedAsButton(form.getFormLayouts().isLocalSignupEmbeddedAsButton());
		FormLayoutUtils.updateRegistrationFormLayout(layouts, form);
		setFormLayouts(layouts);
	}

}
