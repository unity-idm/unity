/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.v8.tokens;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.vaadin.event.selection.SelectionListener;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SearchField;

import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component showing a grid with the oauth tokens.
 * 
 * @author P.Piernik
 *
 */
class OAuthTokenGrid extends CustomComponent
{
	private OAuthTokenController controller;
	private MessageSource msg;

	private GridWithActionColumn<OAuthTokenBean> tokensGrid;

	OAuthTokenGrid(MessageSource msg, OAuthTokenController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	private void initUI()
	{
		tokensGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);

		tokensGrid.addSortableColumn(r -> r.getType(), msg.getMessage("OAuthToken.type"), 10);
		tokensGrid.addSortableColumn(r -> r.getId(), msg.getMessage("OAuthToken.id"), 10);
		tokensGrid.addSortableColumn(r -> r.getOwner(), msg.getMessage("OAuthToken.owner"), 10).setHidable(true)
				.setHidden(false);
		tokensGrid.addSortableColumn(r -> r.getClientName(), msg.getMessage("OAuthToken.clientName"), 10)
				.setHidable(true).setHidden(false);
		tokensGrid.addSortableColumn(r -> r.getCreateTime(), msg.getMessage("OAuthToken.createTime"), 10)
				.setHidable(true).setHidden(true);
		tokensGrid.addSortableColumn(r -> r.getExpires(), msg.getMessage("OAuthToken.expires"), 10)
				.setHidable(true).setHidden(false);
		tokensGrid.addSortableColumn(r -> r.getServerId(), msg.getMessage("OAuthToken.serverId"), 10)
				.setHidable(true).setHidden(true);
		tokensGrid.addSortableColumn(r -> r.getAssociatedRefreshTokenForAccessToken(), msg.getMessage("OAuthToken.refreshToken"), 10)
				.setHidable(true).setHidden(true);
		tokensGrid.addSortableColumn(r -> r.getScopes(), msg.getMessage("OAuthToken.scopes"), 10)
				.setHidable(true).setHidden(false);
		tokensGrid.addSortableColumn(r -> String.valueOf(r.getHasIdToken()),
				msg.getMessage("OAuthToken.hasIdToken"), 10).setHidable(true).setHidden(true);

		tokensGrid.setActionColumnHidden(true);
		tokensGrid.setColumnReorderingAllowed(true);

		tokensGrid.setMultiSelect(true);
		tokensGrid.setSizeFull();

		HamburgerMenu<OAuthTokenBean> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleName(Styles.sidebar.toString());
		hamburgerMenu.addActionHandlers(Arrays.asList(getDeleteAction()));
		tokensGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = FilterableGridHelper.generateSearchField(tokensGrid, msg);
		Toolbar<OAuthTokenBean> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search, Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar reqGridWithToolbar = new ComponentWithToolbar(tokensGrid, toolbar,
				Alignment.BOTTOM_LEFT);
		reqGridWithToolbar.setSizeFull();
		reqGridWithToolbar.setSpacing(false);

		setCompositionRoot(reqGridWithToolbar);
		setSizeFull();
	}

	public void addValueChangeListener(SelectionListener<OAuthTokenBean> listener)
	{
		tokensGrid.addSelectionListener(listener);
	}

	public void setItems(Collection<OAuthTokenBean> items)
	{
		tokensGrid.setItems(items);
	}

	private SingleActionHandler<OAuthTokenBean> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, OAuthTokenBean.class).withHandler(this::deleteHandler)
				.build();
	}

	private void deleteHandler(Collection<OAuthTokenBean> items)
	{
		new ConfirmDialog(msg, msg.getMessage("OAuthTokenGrid.confirmDelete"), () -> {
			for (OAuthTokenBean item : items)
				removeToken(item);
		}).show();

	}

	protected void removeToken(OAuthTokenBean item)
	{
		try
		{
			controller.removeToken(item);
			tokensGrid.removeElement(item);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

}
