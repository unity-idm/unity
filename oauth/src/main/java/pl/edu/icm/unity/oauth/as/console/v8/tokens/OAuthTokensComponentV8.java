/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.console.v8.tokens;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Collection;

/**
 * Allows for viewing and removing tokens
 * 
 * @author P.Piernik
 */
@PrototypeComponent
class OAuthTokensComponentV8 extends CustomComponent
{

	private MessageSource msg;
	private OAuthTokenController controller;
	private OAuthTokenGrid tokensGrid;

	@Autowired
	OAuthTokensComponentV8(MessageSource msg, OAuthTokenController controller)
	{
		this.msg = msg;
		this.controller = controller;
		tokensGrid = new OAuthTokenGrid(msg, controller);
		initUI();
	}

	public OAuthTokensComponentV8 forService(String service)
	{
		Collection<OAuthTokenBean> oAuthTokens = null;
		try
		{
			oAuthTokens = controller.getOAuthTokens(service);
			tokensGrid.setItems(oAuthTokens);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}

		return this;

	}

	private void initUI()
	{
		OAuthTokenViewer viewer = new OAuthTokenViewer(msg);
		viewer.setVisible(false);
		tokensGrid.addValueChangeListener(e -> {
			viewer.setInput(e.getFirstSelectedItem());	
		});

		Panel viewerPanel = new Panel();
		viewerPanel.setContent(viewer);
		viewerPanel.setSizeFull();
		viewerPanel.setStyleName(Styles.vPanelBorderless.toString());

		CompositeSplitPanel splitPanel = new CompositeSplitPanel(true, false, tokensGrid, viewerPanel, 50);
		splitPanel.setSizeFull();

		VerticalLayout main = new VerticalLayout();
		main.addComponent(splitPanel);
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);
		setSizeFull();
	}
}
