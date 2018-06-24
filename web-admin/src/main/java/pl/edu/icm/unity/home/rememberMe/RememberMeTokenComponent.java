/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.rememberMe;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.webui.authn.RemeberMeHelper;
import pl.edu.icm.unity.webui.authn.RemeberMeToken;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;

/**
 * Ui for rememberMe tokens management
 * @author P.Piernik
 *
 */
public class RememberMeTokenComponent extends VerticalLayout 
{
	private SecuredTokensManagement tokenMan;

	private UnityMessageSource msg;

	private Grid<TableTokensBean> tokensTable;

	public RememberMeTokenComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("RememberMeTokenHomeUI.tokensLabel"));
		setMargin(false);
		setSpacing(false);
		tokensTable = new SmallGrid<>();
		tokensTable.setSizeFull();
		tokensTable.setSelectionMode(SelectionMode.MULTI);

		HorizontalLayout buttons = new HorizontalLayout();
		Button removeButton = new Button(msg.getMessage("RememberMeTokenHomeUI.remove"));
		removeButton.setIcon(Images.delete.getResource());
		removeButton.setEnabled(false);

		Button refreshButton = new Button(msg.getMessage("RememberMeTokenHomeUI.refresh"));
		refreshButton.setIcon(Images.refresh.getResource());

		buttons.addComponents(refreshButton, removeButton);
		buttons.setMargin(new MarginInfo(false, false, true, false));

		refreshButton.addClickListener(event -> refresh());

		removeButton.addClickListener(event -> {
			Collection<TableTokensBean> items = tokensTable.getSelectedItems();
			new ConfirmDialog(msg,
					msg.getMessage("RememberMeTokenHomeUI.confirmRemove"),
					() -> {
						for (TableTokensBean item : items)
							removeToken(item.getValue());
					}).show();
		});

		tokensTable.addSelectionListener(event -> {
			Collection<TableTokensBean> items = event.getAllSelectedItems();
			removeButton.setEnabled(items.size() > 0);
		});

		addColumn("ip", TableTokensBean::getIp, false);
		addColumn("browser", TableTokensBean::getBrowser, false);
		addColumn("os", TableTokensBean::getOS, false);
		addColumn("policy", TableTokensBean::getPolicy, false);
		addColumn("createTime", TableTokensBean::getCreateTime, false);
		addColumn("expires", TableTokensBean::getExpires, false);

		SingleActionHandler<TableTokensBean> refreshAction = getRefreshAction();
		SingleActionHandler<TableTokensBean> deleteAction = getDeleteAction();

		GridContextMenuSupport<TableTokensBean> contextMenu = new GridContextMenuSupport<>(
				tokensTable);
		contextMenu.addActionHandler(refreshAction);
		contextMenu.addActionHandler(deleteAction);
		GridSelectionSupport.installClickListener(tokensTable);

		addComponent(buttons);
		addComponent(tokensTable);
		refresh();
	}

	private void addColumn(String key, ValueProvider<TableTokensBean, String> valueProvider,
			boolean hidden)
	{
		tokensTable.addColumn(valueProvider, t -> t)
				.setCaption(msg.getMessage("RememberMeToken." + key))
				.setHidable(true).setId(key).setHidden(hidden);
	}

	private SingleActionHandler<TableTokensBean> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, TableTokensBean.class)
				.withHandler(selection -> refresh()).build();
	}

	private SingleActionHandler<TableTokensBean> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, TableTokensBean.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<TableTokensBean> items)
	{
		new ConfirmDialog(msg, msg.getMessage("RememberMeTokenHomeUI.confirmRemove"),
				() -> {
					for (TableTokensBean item : items)
						removeToken(item.getValue());
				}).show();

	}

	protected void refresh()
	{
		try
		{
			List<Token> tokens = tokenMan
					.getOwnedTokens(RemeberMeHelper.REMEMBER_ME_TOKEN_TYPE);
			tokensTable.setItems(tokens.stream().map(t -> new TableTokensBean(t, msg)));
			tokensTable.deselectAll();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("RememberMeTokenHomeUI.errorGetTokens"), e);
		}

	}

	protected boolean removeToken(String value)
	{
		try
		{
			tokenMan.removeToken(RemeberMeHelper.REMEMBER_ME_TOKEN_TYPE, value);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("RememberMeTokenHomeUI.errorRemove"), e);
			return false;
		}
	}

	public static class TableTokensBean
	{
		private Token token;

		private RemeberMeToken rememberMeToken;

		private UnityMessageSource msg;

		public TableTokensBean(Token token, UnityMessageSource msg)
		{
			this.token = token;
			this.msg = msg;
			this.rememberMeToken = RemeberMeToken
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

		Token getToken()
		{
			return token;
		}

		RemeberMeToken getOAuthToken()
		{
			return rememberMeToken;
		}
	}
}
