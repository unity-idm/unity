/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.credentials;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.RememberMeProcessor;
import pl.edu.icm.unity.webui.authn.RememberMeToken;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Ui for rememberMe tokens management
 * @author P.Piernik
 *
 */
public class TrustedDevicesComponent extends CustomComponent 
{
	private TokensManagement tokenMan;
	private UnityMessageSource msg;
	private Grid<TableTokensBean> tokensTable;
	private long entityId;

	public TrustedDevicesComponent(TokensManagement tokenMan, UnityMessageSource msg, long entityId)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.entityId = entityId;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		tokensTable = new SmallGrid<>();
		tokensTable.setWidth(100, Unit.PERCENTAGE);
		tokensTable.setSelectionMode(SelectionMode.MULTI);
		
		
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

		Toolbar<TableTokensBean> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		tokensTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(contextMenu.getActionHandlers());
		
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(tokensTable, toolbar);
		tableWithToolbar.setWidth(100, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(15, Unit.EM);
		
		main.addComponent(tableWithToolbar);
		refresh();
		setCompositionRoot(main);
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
		new ConfirmDialog(msg, msg.getMessage("TrustedDevicesComponent.confirmRemove"),
				() -> {
					for (TableTokensBean item : items)
						removeToken(item.getValue());
				}).show();

	}

	public void removeAll()
	{
		List<Token> tokens = null;
		try
		{
			tokens = tokenMan.getOwnedTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE,
					new EntityParam(entityId));
		}

		catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("TrustedDevicesComponent.errorGetTokens"),
					e);
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
			NotificationPopup.showError(msg,
					msg.getMessage("TrustedDevicesComponent.errorRemove"), e);
			return;
		}
	}

	private void refresh()
	{
		try
		{
			List<Token> tokens = tokenMan
					.getOwnedTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE, new EntityParam(entityId));
			tokensTable.setItems(tokens.stream().map(t -> new TableTokensBean(t, msg)));
			tokensTable.deselectAll();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("TrustedDevicesComponent.errorGetTokens"), e);
		}

	}

	private boolean removeToken(String value)
	{
		try
		{
			tokenMan.removeToken(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE, value);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("TrustedDevicesComponent.errorRemove"), e);
			return false;
		}
	}

	public static class TableTokensBean
	{
		private Token token;
		private RememberMeToken rememberMeToken;
		private UnityMessageSource msg;

		public TableTokensBean(Token token, UnityMessageSource msg)
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

		Token getToken()
		{
			return token;
		}

		RememberMeToken getOAuthToken()
		{
			return rememberMeToken;
		}
	}
}
