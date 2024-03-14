/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.additional;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

class AdditionalAuthnDialog extends Dialog
{
	private final AuthNPanel authnPanel;
	private final Runnable cancelHandler;
	private Registration addCloseListener;

	AdditionalAuthnDialog(String caption, String info, AuthNPanel authnPanel,
			Runnable cancelHandler)
	{
		this.authnPanel = authnPanel;
		this.cancelHandler = cancelHandler;
		initGUI(caption, info);
	}

	private void initGUI(String caption, String info)
	{
		setModal(true);

		Div contentsPanel = new Div();
		contentsPanel.setSizeFull();
		VerticalLayout internal = new VerticalLayout();

		Span header = new Span(caption);
		internal.add(header);
		internal.setAlignItems(FlexComponent.Alignment.CENTER);

		Span infoL = new Span(info);
		infoL.setWidthFull();
		internal.add(infoL);

		internal.add(new Span(""));
		
		internal.add(authnPanel);
		authnPanel.setWidth(VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH, Unit.EM);

		contentsPanel.add(internal);

		add(contentsPanel);
		
		setWidth(32, Unit.EM);
		setHeight(20, Unit.EM);
		
		addCloseListener = addDialogCloseActionListener(event -> cancelHandler.run());
	}
	
	void show()
	{
		open();
		authnPanel.focusIfPossible();
	}

	public void diableCancelListener()
	{
		addCloseListener.remove();
	}
}
