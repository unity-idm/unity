/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Standalone view for sticky enquiry. Show remove last request button if
 * request for sticky form already exists
 * 
 * @author P.Piernik
 *
 */
public class StandaloneStickyEnquiryView extends StandaloneEnquiryView
{
	private Runnable removeCallback;

	StandaloneStickyEnquiryView(EnquiryResponseEditor editor, StandardWebAuthenticationProcessor authnProcessor,
			UnityMessageSource msg, Callback callback, Runnable removeCallback)
	{
		super(editor, authnProcessor, msg, callback);
		this.removeCallback = removeCallback;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		if (editor.getPageTitle() != null)
			Page.getCurrent().setTitle(editor.getPageTitle());
		placeRemoveLastRequestQuestionView();
	}

	private void placeRemoveLastRequestQuestionView()
	{
		main.removeAllComponents();
		main.setHeight(100, Unit.PERCENTAGE);
		setSizeFull();	
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(true);
		wrapper.setMargin(false);
		wrapper.setWidth(100, Unit.PERCENTAGE);
		
		Label info = new Label(msg.getMessage("StandaloneStickyEnquiryView.overwriteRequestInfo"));
		info.addStyleName(Styles.vLabelLarge.toString());
		info.addStyleName("u-reg-title");
		wrapper.addComponent(info);
		wrapper.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
		
		Component buttons = createFirstStepButtonsBar();
		wrapper.addComponent(buttons);
		wrapper.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
		
		main.addComponent(wrapper);
		main.setComponentAlignment(wrapper, Alignment.MIDDLE_CENTER);
	}

	private Component createFirstStepButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button removeLast = new Button(msg.getMessage("StandaloneStickyEnquiryView.removeLastRequest"));
		removeLast.addStyleName(Styles.vButtonPrimary.toString());
		removeLast.addClickListener(event -> {
			removeCallback.run();
			placeEditor();
		});
		
		buttons.addComponents( getCancellButton(), removeLast);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		return buttons;
	}
}
