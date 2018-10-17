/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.additional;

import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

class AdditionalAuthnDialog extends Window
{
	private AuthNPanel authnPanel;
	private Runnable cancelHandler;
	private Registration addCloseListener;

	AdditionalAuthnDialog(UnityMessageSource msg, String caption, String info, AuthNPanel authnPanel, 
			Runnable cancelHandler)
	{
		this.authnPanel = authnPanel;
		this.cancelHandler = cancelHandler;
		initGUI(caption, info);
	}

	private void initGUI(String caption, String info)
	{
		setModal(true);
		setClosable(true);

		Panel contentsPanel = new SafePanel();
		contentsPanel.setSizeFull();
		contentsPanel.addStyleName(Styles.vPanelLight.toString());
		VerticalLayout internal = new VerticalLayout();
		
		Label header = new Label(caption);
		internal.addComponent(header);
		internal.setComponentAlignment(header, Alignment.MIDDLE_CENTER);

		Label infoL = new Label(info);
		infoL.setWidth(100, Unit.PERCENTAGE);
		internal.addComponent(infoL);
		internal.setComponentAlignment(header, Alignment.TOP_LEFT);
		
		internal.addComponent(new Label(""));
		
		internal.addComponent(authnPanel);
		authnPanel.setWidth(VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH, Unit.EM);
		internal.setComponentAlignment(authnPanel, Alignment.TOP_CENTER);
		
		contentsPanel.setContent(internal);
		contentsPanel.addStyleName(Styles.centeredPanel.toString());
		
		setContent(contentsPanel);
		
		setWidth(32, Unit.EM);
		setHeight(20, Unit.EM);
		
		addCloseListener = addCloseListener(event -> cancelHandler.run());
	}
	
	void show()
	{
		UI.getCurrent().addWindow(this);
		authnPanel.focusIfPossible();
	}

	public void diableCancelListener()
	{
		addCloseListener.remove();
	}
}
