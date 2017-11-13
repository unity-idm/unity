/*
e * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.SmallTableDeprecated;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Allows for viewing and removing tokens 
 * @author P.Piernik
 *
 */

public class AdminTokensComponent extends VerticalLayout
{	

	private AttributeSupport attrProcessor;
	private AttributesManagement attrMan;
	protected SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	
	protected VerticalLayout main;
	protected VerticalLayout tokensTablePanel;
	protected ComponentWithToolbar tableWithToolbar;
	protected Table tokensTable;
	private OAuthTokenViewer viewer;
	private boolean showViewer;
	
	
	public AdminTokensComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, AttributesManagement attrMan, boolean showViewer)
	{
		
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.attrMan = attrMan;
		this.attrProcessor = attrProcessor;
		this.showViewer = showViewer;
		initUI();
	}
	
	private void initUI()

	{
		setCaption(msg.getMessage("OAuthTokenAdminUI.tokensLabel"));
		
		tokensTable = new SmallTableDeprecated();
		tokensTable.setNullSelectionAllowed(false);
		tokensTable.setImmediate(true);
		tokensTable.setSizeFull();
		BeanItemContainer<TableTokensBean> tableContainer = new BeanItemContainer<>(TableTokensBean.class);
		tableContainer.removeContainerProperty("element");
		tokensTable.setSelectable(true);
		tokensTable.setMultiSelect(true);
		tokensTable.setContainerDataSource(tableContainer);
		tokensTable.setVisibleColumns(new Object[] { "type", "value", "owner", "clientName", "createTime",
				"expires","scopes" ,"serverId", "refreshToken", "hasIdToken" });
		tokensTable.setColumnHeaders(new String[] { msg.getMessage("OAuthToken.type"),
				msg.getMessage("OAuthToken.value"),
				msg.getMessage("OAuthToken.owner"),
				msg.getMessage("OAuthToken.clientName"),
				msg.getMessage("OAuthToken.createTime"),
				msg.getMessage("OAuthToken.expires"),
				msg.getMessage("OAuthToken.scopes"),
				msg.getMessage("OAuthToken.serverId"),
				msg.getMessage("OAuthToken.refreshToken"),
				msg.getMessage("OAuthToken.hasIdToken")});
		tokensTable.setSortContainerPropertyId(
				tokensTable.getContainerPropertyIds().iterator().next());
		tokensTable.setSortAscending(true);
		tokensTable.setColumnCollapsingAllowed(true);
		tokensTable.setColumnCollapsed("serverId", true); 
		tokensTable.setColumnCollapsed("hasIdToken", true); 

		viewer = new OAuthTokenViewer(msg);
		viewer.setVisible(false);
		tokensTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{

				Collection<?> items = (Collection<?>) tokensTable
						.getValue();

				if (items.size() > 1 || items.isEmpty())

				{
					viewer.setInput(null, null);
					viewer.setVisible(false);
					return;

				}

				TableTokensBean item = (TableTokensBean) items.iterator().next();
				viewer.setVisible(true);
				viewer.setInput(item.getOAuthToken(), item.getToken());
			}
		});
		
		RefreshActionHandler refHandler = new RefreshActionHandler();
		DeleteActionHandler delHandler = new DeleteActionHandler();
		tokensTable.addActionHandler(refHandler);
		tokensTable.addActionHandler(delHandler);
		
		tokensTablePanel = new VerticalLayout();
		tokensTablePanel.addComponent(tokensTable);
			
		Toolbar toolbar = new Toolbar(tokensTable, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(refHandler, delHandler);
			
		tableWithToolbar = new ComponentWithToolbar(tokensTablePanel, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		
		main = new VerticalLayout();
		if (showViewer)
		{

			CompositeSplitPanel hl = new CompositeSplitPanel(false, true,
					tableWithToolbar, viewer, 60);
			hl.setSizeFull();
			main.addComponent(hl);

		} else
		{
			main.addComponent(tableWithToolbar);
		}
		main.setSizeFull();
		refresh();
		
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("OAuthTokenAdminUI.refreshAction"),
					Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("OAuthTokenAdminUI.deleteAction"),
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<?> items = (Collection<?>) tokensTable.getValue();
			

			new ConfirmDialog(msg, msg.getMessage(
					"OAuthTokenAdminUI.confirmDelete"),new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (Object item : items)
					{
						TableTokensBean tokenBean = (TableTokensBean) item;
						removeToken(tokenBean.getRealType(), tokenBean.getValue());
					}
				}
			}).show();

		}
	}
	
	protected boolean removeToken(String type, String value)
	{
		try
		{
			tokenMan.removeToken(type, value);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("OAuthTokenAdminUI.errorRemove"),
					e);
			return false;
		}
	}

	
	protected List<Token> getTokens() throws EngineException
	{
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getAllTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getAllTokens(OAuthProcessor.INTERNAL_REFRESH_TOKEN));
		return tokens;
	}
	
	protected void refresh()
	{
		try
		{
			tokensTable.removeAllItems();
			for (Token t: getTokens())
			{
				TableTokensBean item = new TableTokensBean(t, msg, attrProcessor, attrMan);
				tokensTable.addItem(item);
			}
		
			tokensTable.setValue(null);
			viewer.setInput(null, null);
			viewer.setVisible(false);
			removeAllComponents();
			addComponent(main);
			
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("OAuthTokenAdminUI.errorGetTokens"),
					e);
		}
		
	}
	
	public static class TableTokensBean
	{
		private Token token;
		private OAuthToken oauthToken;
		private UnityMessageSource msg;
		private AttributeSupport attrProcessor;
		private AttributesManagement attrMan;
		
		
		public TableTokensBean(Token token, UnityMessageSource msg,
				AttributeSupport attrProcessor, AttributesManagement attrMan) throws JsonParseException, JsonMappingException, IOException
		{
			super();
			this.token = token;
			this.msg = msg;
			this.attrProcessor = attrProcessor;
			this.attrMan = attrMan;
			this.oauthToken = OAuthToken.getInstanceFromJson(token.getContents());
		}
	
		public String getType()
		{
			try
			{
				return msg.getMessage("OAuthTokenType." + token.getType());
			} catch (Exception e)
			{
				return token.getType();
			}
		}
		
		public String getRealType()
		{
			return token.getType();
		}
		
		public String getOwner()
		{
			long ownerId = token.getOwner();
			String idLabel = "[" + ownerId + "]";
			String attrNameValue = getAttrNameValue(ownerId);
			if (attrNameValue != null)
				idLabel = idLabel + " " + attrNameValue;
			return idLabel;
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
		
		public String getServerId()
		{
			return oauthToken.getIssuerUri();
		}
		
		public String getRefreshToken()
		{
			//show refresh token only for access token
			return oauthToken.getRefreshToken() != null && !token.getType().equals(OAuthProcessor.INTERNAL_REFRESH_TOKEN) ? oauthToken.getRefreshToken() : "";
		}
		
		public boolean getHasIdToken()
		{
			return oauthToken.getOpenidInfo() != null;
		}
		
		public String getClientName()
		{
			return oauthToken.getClientName() != null
					&& !oauthToken.getClientName().isEmpty()
							? oauthToken.getClientName()
							: oauthToken.getClientUsername();
		}

		public String getScopes()
		{
			return Stream.of(oauthToken.getEffectiveScope()).collect(Collectors.joining(", "));
		}
		
		private String getAttrNameValue(long owner)
		{
			String idLabel = null;
			try
			{
				AttributeType nameAt = attrProcessor.getAttributeTypeWithSingeltonMetadata(
						EntityNameMetadataProvider.NAME);
				String attrToLabel = nameAt == null ? null : nameAt.getName();
				Collection<AttributeExt> rootAttrs = new ArrayList<AttributeExt>();
				rootAttrs = attrMan.getAllAttributes(new EntityParam(owner), true, "/",
						null, true);
				if (attrToLabel != null)
					for (AttributeExt at : rootAttrs)
					{
						if (at.getName().equals(attrToLabel))
							idLabel = at.getValues().get(0);
					}
			} catch (EngineException e)
			{
				// ok only user id is present in label
			}
			return idLabel;
		}
		
		public Token getToken()
		{
			return token;
		}
		
		public OAuthToken getOAuthToken()
		{
			return oauthToken;
		}
		
		

	
		
		
	}
}
