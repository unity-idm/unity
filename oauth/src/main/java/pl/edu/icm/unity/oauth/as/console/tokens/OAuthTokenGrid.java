/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.selection.SelectionListener;
import io.imunity.vaadin.elements.ColumnToggleMenu;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

import java.util.Collection;
import java.util.Collections;

/**
 * Component showing a grid with the oauth tokens.
 * 
 * @author P.Piernik
 *
 */
class OAuthTokenGrid extends VerticalLayout
{
	private final OAuthTokenController controller;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;

	private GridWithActionColumn<OAuthTokenBean> tokensGrid;

	OAuthTokenGrid(MessageSource msg, OAuthTokenController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		ColumnToggleMenu columnToggleMenu = new ColumnToggleMenu();

		tokensGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());

		tokensGrid.addColumn(OAuthTokenBean::getType)
				.setHeader(msg.getMessage("OAuthToken.type"))
				.setSortable(true)
				.setAutoWidth(true);
		tokensGrid.addColumn(OAuthTokenBean::getId)
				.setHeader(msg.getMessage("OAuthToken.id"))
				.setSortable(true)
				.setAutoWidth(true);
		Grid.Column<OAuthTokenBean> ownerColumn = tokensGrid.addColumn(OAuthTokenBean::getOwner)
				.setHeader(msg.getMessage("OAuthToken.owner"))
				.setSortable(true)
				.setAutoWidth(true);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.owner"), ownerColumn);

		Grid.Column<OAuthTokenBean> clientColumn = tokensGrid.addColumn(OAuthTokenBean::getClientName)
				.setHeader(msg.getMessage("OAuthToken.clientName"))
				.setSortable(true)
				.setAutoWidth(true);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.clientName"), clientColumn);

		Grid.Column<OAuthTokenBean> createTimeColumn = tokensGrid.addColumn(OAuthTokenBean::getCreateTime)
				.setHeader(msg.getMessage("OAuthToken.createTime"))
				.setSortable(true)
				.setAutoWidth(true);
		createTimeColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.createTime"), createTimeColumn);

		Grid.Column<OAuthTokenBean> expiresColumn = tokensGrid.addColumn(OAuthTokenBean::getExpires)
				.setHeader(msg.getMessage("OAuthToken.expires"))
				.setSortable(true)
				.setAutoWidth(true);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.expires"), expiresColumn);

		Grid.Column<OAuthTokenBean> serverIdColumn = tokensGrid.addColumn(OAuthTokenBean::getServerId)
				.setHeader(msg.getMessage("OAuthToken.serverId"))
				.setSortable(true)
				.setAutoWidth(true);
		serverIdColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.serverId"), serverIdColumn);

		Grid.Column<OAuthTokenBean> refreshTokenColumn = tokensGrid.addColumn(
						OAuthTokenBean::getAssociatedRefreshTokenForAccessToken)
				.setHeader(msg.getMessage("OAuthToken.refreshToken"))
				.setSortable(true)
				.setAutoWidth(true);
		refreshTokenColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.refreshToken"), refreshTokenColumn);

		Grid.Column<OAuthTokenBean> scopesColumn = tokensGrid.addColumn(OAuthTokenBean::getScopes)
				.setHeader(msg.getMessage("OAuthToken.scopes"))
				.setSortable(true)
				.setAutoWidth(true);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.scopes"), scopesColumn);

		Grid.Column<OAuthTokenBean> hasIdTokenColumn = tokensGrid.addColumn(r -> String.valueOf(r.getHasIdToken()))
				.setHeader(msg.getMessage("OAuthToken.hasIdToken"))
				.setSortable(true)
				.setAutoWidth(true);
		hasIdTokenColumn.setVisible(false);
		columnToggleMenu.addColumn(msg.getMessage("OAuthToken.hasIdToken"), hasIdTokenColumn);
		tokensGrid.setActionColumnHeader(columnToggleMenu.getTarget());
		tokensGrid.setColumnReorderingAllowed(true);
		tokensGrid.setMultiSelect(true);

		ActionMenuWithHandlerSupport<OAuthTokenBean> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(Collections.singletonList(getDeleteAction()));
		tokensGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = GridSearchFieldFactory.generateSearchField(tokensGrid, msg::getMessage);
		Toolbar<OAuthTokenBean> toolbar = new Toolbar<>();
		toolbar.setWidthFull();
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search);
		ComponentWithToolbar reqGridWithToolbar = new ComponentWithToolbar(tokensGrid, toolbar);
		reqGridWithToolbar.setWidthFull();
		reqGridWithToolbar.setSpacing(false);

		add(reqGridWithToolbar, tokensGrid);
		setWidthFull();
	}

	public void addValueChangeListener(SelectionListener<Grid<OAuthTokenBean>, OAuthTokenBean> listener)
	{
		tokensGrid.addSelectionListener(listener);
	}

	public void setItems(Collection<OAuthTokenBean> items)
	{
		tokensGrid.setItems(items);
	}

	private SingleActionHandler<OAuthTokenBean> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg::getMessage, OAuthTokenBean.class).withHandler(this::deleteHandler)
				.build();
	}

	private void deleteHandler(Collection<OAuthTokenBean> items)
	{
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("OAuthTokenGrid.confirmDelete"),
				msg.getMessage("ok"),
				e -> items.forEach(this::removeToken),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	protected void removeToken(OAuthTokenBean item)
	{
		try
		{
			controller.removeToken(item);
			tokensGrid.removeElement(item);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
	}

}
