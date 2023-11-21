/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.identities.credentials;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.ValueProvider;
import io.imunity.console.views.directory_browser.GridSelectionSupport;
import io.imunity.vaadin.elements.ColumnToggleMenu;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessorEE8;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken;
import pl.edu.icm.unity.engine.api.token.TokensManagement;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import static io.imunity.vaadin.elements.CssClassNames.DISABLED_ICON;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;


class TrustedDevicesComponent extends VerticalLayout
{
	private final NotificationPresenter notificationPresenter;
	private final TokensManagement tokenMan;
	private final MessageSource msg;
	private Grid<TableTokensBean> tokensGrid;
	private ColumnToggleMenu columnToggleMenu;
	private final long entityId;

	TrustedDevicesComponent(TokensManagement tokenMan, MessageSource msg, long entityId,
			NotificationPresenter notificationPresenter)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.entityId = entityId;
		initUI();
	}

	private void initUI()
	{
		setPadding(false);
		tokensGrid = new Grid<>();
		tokensGrid.setWidthFull();
		tokensGrid.setSelectionMode(Grid.SelectionMode.MULTI);

		columnToggleMenu = new ColumnToggleMenu();
		addColumn("ip", TableTokensBean::getIp);
		addColumn("browser", TableTokensBean::getBrowser);
		addColumn("os", TableTokensBean::getOS);
		addColumn("policy", TableTokensBean::getPolicy);
		addColumn("createTime", TableTokensBean::getCreateTime);
		addColumn("expires", TableTokensBean::getExpires);
		tokensGrid.addColumn(e -> "")
				.setHeader(columnToggleMenu.getTarget())
				.setTextAlign(ColumnTextAlign.END)
				.setAutoWidth(true);

		GridSelectionSupport.installClickListener(tokensGrid);

		HorizontalLayout upperLayout = new HorizontalLayout(getRefreshAction(), getDeleteAction());
		upperLayout.setWidthFull();
		upperLayout.setJustifyContentMode(JustifyContentMode.END);
		add(upperLayout, tokensGrid);
		refresh();
	}

	private void addColumn(String key, ValueProvider<TableTokensBean, String> valueProvider)
	{
		Grid.Column<TableTokensBean> column = tokensGrid.addColumn(valueProvider)
				.setHeader(msg.getMessage("RememberMeToken." + key))
				.setAutoWidth(true);
		columnToggleMenu.addColumn(msg.getMessage("RememberMeToken." + key), column);
	}

	private Icon getRefreshAction()
	{
		Icon icon = VaadinIcon.REFRESH.create();
		icon.addClassName(POINTER.getName());
		icon.addClickListener(event -> refresh());
		return icon;
	}

	private Icon getDeleteAction()
	{
		Icon icon = VaadinIcon.TRASH.create();
		icon.setClassName(DISABLED_ICON.getName());
		tokensGrid.addSelectionListener(e -> {
			if(e.getAllSelectedItems().isEmpty())
				icon.setClassName(DISABLED_ICON.getName());
			else
				icon.setClassName(POINTER.getName());
		});
		icon.addClickListener(event -> deleteHandler(tokensGrid.getSelectedItems()));
		return icon;
	}

	private void deleteHandler(Collection<TableTokensBean> items)
	{
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("TrustedDevicesComponent.confirmRemove"),
				msg.getMessage("ok"),
				e ->
				{
					for (TableTokensBean item : items)
						removeToken(item.getValue());
				},
				msg.getMessage("cancel"),
				e ->
				{
				}
		).open();
	}

	public void removeAll()
	{
		List<Token> tokens;
		try
		{
			tokens = tokenMan.getOwnedTokens(RememberMeProcessorEE8.REMEMBER_ME_TOKEN_TYPE,
					new EntityParam(entityId));
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("TrustedDevicesComponent.errorGetTokens"),
					e.getMessage());
			return;
		}

		try
		{
			for (Token token : tokens)
			{
				tokenMan.removeToken(token.getType(), token.getValue());
			}
			refresh();

		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("TrustedDevicesComponent.errorRemove"), e.getMessage());
		}
	}

	private void refresh()
	{
		try
		{
			List<Token> tokens = tokenMan
					.getOwnedTokens(RememberMeProcessorEE8.REMEMBER_ME_TOKEN_TYPE, new EntityParam(entityId));
			tokensGrid.setItems(tokens.stream().map(t -> new TableTokensBean(t, msg)).toList());
			tokensGrid.deselectAll();
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("TrustedDevicesComponent.errorGetTokens"), e.getMessage());
		}

	}

	private void removeToken(String value)
	{
		try
		{
			tokenMan.removeToken(RememberMeProcessorEE8.REMEMBER_ME_TOKEN_TYPE, value);
			refresh();
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("TrustedDevicesComponent.errorRemove"), e.getMessage());
		}
	}

	public static class TableTokensBean
	{
		private final Token token;
		private final RememberMeToken rememberMeToken;
		private final MessageSource msg;

		public TableTokensBean(Token token, MessageSource msg)
		{
			this.token = token;
			this.msg = msg;
			this.rememberMeToken = RememberMeToken
					.getInstanceFromJson(token.getContents());
		}

		public String getCreateTime()
		{
			return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
					.format(token.getCreated());
		}

		public String getExpires()
		{
			return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
					.format(token.getExpires());
		}

		public String getValue()
		{
			return token.getValue();
		}

		public String getIp()
		{
			return rememberMeToken.getMachineDetails().getIp();
		}

		public String getBrowser()
		{
			return rememberMeToken.getMachineDetails().getBrowser();
		}

		public String getOS()
		{
			return rememberMeToken.getMachineDetails().getOs();
		}

		public String getPolicy()
		{
			try
			{
				return msg.getMessage("RememberMePolicy."
						+ rememberMeToken.getRememberMePolicy().toString());
			} catch (Exception e)
			{
				return rememberMeToken.getRememberMePolicy().toString();
			}
		}
	}
}
