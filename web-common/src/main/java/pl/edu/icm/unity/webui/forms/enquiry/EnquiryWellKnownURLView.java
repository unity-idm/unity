/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

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
	private UnityServerConfiguration cfg;
	
	public EnquiryWellKnownURLView(EnquiryResponseEditor editor, UnityMessageSource msg, 
			UnityServerConfiguration cfg, Callback callback)
	{
		this.editor = editor;
		this.msg = msg;
		this.cfg = cfg;
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
		LocaleChoiceComponent localeChoice = new LocaleChoiceComponent(cfg, msg);
		
		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidthUndefined();
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			if (callback.submitted())
				showConfirm(Images.ok32.getResource(),
					msg.getMessage("EnquiryWellKnownURLView.responseSubmitted"));
		});
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addClickListener(event -> {
			callback.cancelled();
			showConfirm(Images.error32.getResource(), 
					msg.getMessage("EnquiryWellKnownURLView.responseCancelled"));
		});
		buttons.addComponents(cancel, ok);
		buttons.setSpacing(true);
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);		
	}

	
	private void initUIBase()
	{
		main = new VerticalLayout();
		main.setMargin(true);
		main.setSpacing(true);
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	private void showConfirm(Resource icon, String message)
	{
		VerticalLayout wrapper = new VerticalLayout();
		ConfirmationComponent confirmation = new ConfirmationComponent(icon, message);
		wrapper.addComponent(confirmation);
		wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);
	}
	
	public interface Callback
	{
		boolean submitted();
		void cancelled();
	}
}
