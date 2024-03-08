/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.console.tokens;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Collection;

/**
 * Allows for viewing and removing tokens
 * 
 * @author P.Piernik
 */
@PrototypeComponent
class OAuthTokensComponent extends VerticalLayout
{

	private final MessageSource msg;
	private final OAuthTokenController controller;
	private final OAuthTokenGrid tokensGrid;
	private final NotificationPresenter notificationPresenter;

	OAuthTokensComponent(MessageSource msg, OAuthTokenController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		tokensGrid = new OAuthTokenGrid(msg, controller, notificationPresenter);
		initUI();
	}

	public OAuthTokensComponent forService(String service)
	{
		Collection<OAuthTokenBean> oAuthTokens = null;
		try
		{
			oAuthTokens = controller.getOAuthTokens(service);
			tokensGrid.setItems(oAuthTokens);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}

		return this;

	}

	private void initUI()
	{
		OAuthTokenViewer viewer = new OAuthTokenViewer(msg);
		viewer.setVisible(false);
		tokensGrid.addValueChangeListener(e -> viewer.setInput(e.getFirstSelectedItem()));

		Panel viewerPanel = new Panel();
		viewerPanel.setMargin(false);
		viewerPanel.add(viewer);
		viewerPanel.setSizeFull();

		SplitLayout splitPanel = new SplitLayout(tokensGrid, viewerPanel);
		splitPanel.setOrientation(SplitLayout.Orientation.VERTICAL);
		splitPanel.setSplitterPosition(50);
		splitPanel.setWidthFull();
		splitPanel.setHeight("40em");

		VerticalLayout main = new VerticalLayout();
		main.add(splitPanel);
		main.setSizeFull();
		main.setPadding(false);
		add(main);
		setSizeFull();
	}
}
