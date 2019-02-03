/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedWithLogoutComponent;

/**
 * Standalone view presenting enquiry form.
 * 
 * @author K. Benedyczak
 */
class StandaloneEnquiryView extends CustomComponent implements View
{
	protected EnquiryResponseEditor editor;
	private Callback callback;
	protected UnityMessageSource msg;
	private StandardWebAuthenticationProcessor authnProcessor;
	protected VerticalLayout main;
	
	StandaloneEnquiryView(EnquiryResponseEditor editor, StandardWebAuthenticationProcessor authnProcessor,
			UnityMessageSource msg,	Callback callback)
	{
		this.editor = editor;
		this.authnProcessor = authnProcessor;
		this.msg = msg;
		this.callback = callback;
		main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
	}
	
	@Override
	public void enter(ViewChangeEvent event)
	{	
		if (editor.getPageTitle() != null)
			Page.getCurrent().setTitle(editor.getPageTitle());
		placeEditor();
	}

	protected void placeEditor()
	{	
		main.removeAllComponents();
		main.setHeightUndefined();
		setHeightUndefined();
		
		Component logout = createLogoutComponent();
		main.addComponent(logout);
		main.setComponentAlignment(logout, Alignment.TOP_RIGHT);
		
		Label filler = new Label("&nbsp", ContentMode.HTML);
		filler.addStyleName("u-verticalSpace1Unit");
		main.addComponent(filler);
		
		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
	
		Component buttons = createButtonsBar();
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);	
	}

	private Component createLogoutComponent()
	{
		Button logout = new Button(msg.getMessage("EnquiryWellKnownURLView.resignLogout"));
		logout.addStyleName(Styles.vButtonLink.toString());
		logout.addClickListener(event -> {
			authnProcessor.logout();
		});
		return logout;
	}

	protected Component createButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(editor.formWidth(), editor.formWidthUnit());
		
		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.setWidth(100, Unit.PERCENTAGE);
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			WorkflowFinalizationConfiguration config = callback.submitted();
			gotoFinalStep(config);
		});
		
		buttons.addComponents(getCancellButton(), ok);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		return buttons;
	}
	
	protected Button getCancellButton()
	{
		String exitMessage = !editor.isOptional() ? msg.getMessage("cancel") 
				: msg.getMessage("EnquiryFormFillDialog.ignore");
		Button ignore = new Button(exitMessage);
		ignore.setWidth(100, Unit.PERCENTAGE);
		ignore.addClickListener(event -> {
			WorkflowFinalizationConfiguration config = callback.cancelled();
			gotoFinalStep(config);
		});
		return ignore;
	}
	
	private void gotoFinalStep(WorkflowFinalizationConfiguration config)
	{
		if (config == null)
			return;
		if (config.autoRedirect)
			redirect(config.redirectURL);
		else
			showFinalScreen(config);
	}
	
	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);

		Component finalScreen = new WorkflowCompletedWithLogoutComponent(config, this::redirect, 
				msg.getMessage("MainHeader.logout"), authnProcessor::logout);
		wrapper.addComponent(finalScreen);
		wrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
	}
	
	private void redirect(String redirectUrl)
	{
		Page.getCurrent().open(redirectUrl, null);
	}
	
	public interface Callback
	{
		WorkflowFinalizationConfiguration submitted();
		WorkflowFinalizationConfiguration cancelled();
	}
}
