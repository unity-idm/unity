/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeader;

/**
 * Standalone view presenting enquiry form.
 * 
 * @author K. Benedyczak
 */
public class EnquiryWellKnownURLView extends CustomComponent implements View
{
	private VerticalLayout main;
	private EnquiryResponseEditor editor;
	private Callback callback;
	private UnityMessageSource msg;
	private WebAuthenticationProcessor authnProcessor;
	
	public EnquiryWellKnownURLView(EnquiryResponseEditor editor, WebAuthenticationProcessor authnProcessor,
			UnityMessageSource msg,	Callback callback)
	{
		this.editor = editor;
		this.authnProcessor = authnProcessor;
		this.msg = msg;
		this.callback = callback;
	}
	
	@Override
	public void enter(ViewChangeEvent event)
	{
		initUIBase();
		placeEditor(editor);
	}

	private void placeEditor(EnquiryResponseEditor editor)
	{
		main.addComponent(editor);
		editor.setWidthUndefined();
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			if (callback.submitted())
				showConfirm(Images.ok,
					msg.getMessage("EnquiryWellKnownURLView.responseSubmitted"));
		});
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addClickListener(event -> {
			callback.cancelled();
			showConfirm(Images.error, 
					msg.getMessage("EnquiryWellKnownURLView.responseCancelled"));
		});
		buttons.addComponents(cancel, ok);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);		
	}

	
	private void initUIBase()
	{
		main = new VerticalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
		
		TopHeader header = new TopHeader("", authnProcessor, msg);
		main.addComponent(header);
	}
	
	private void showConfirm(Images icon, String message)
	{
		VerticalLayout wrapper = new VerticalLayout();
		TopHeader header = new TopHeader("", authnProcessor, msg);
		wrapper.addComponent(header);
		
		ConfirmationComponent confirmation = new ConfirmationComponent(icon, message);
		wrapper.addComponent(confirmation);
		wrapper.setExpandRatio(confirmation, 2f);
		wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
		wrapper.setSizeFull();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		setSizeFull();
		setCompositionRoot(wrapper);
	}
	
	public interface Callback
	{
		boolean submitted();
		void cancelled();
	}
}
