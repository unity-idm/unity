/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Allows for viewing and removing tokens 
 * @author P.Piernik
 *
 */

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TokensComponent extends VerticalLayout
{	
	public static final String INTERNAL_ACCESS_TOKEN = "oauth2Access";
	public static final String INTERNAL_REFRESH_TOKEN = "oauth2Refresh";

	private AttributeSupport attrProcessor;
	private AttributesManagement attrMan;
	protected SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	
	
	protected GenericElementsTable<Token> table;
	protected TokenViewer viewer;
	protected VerticalLayout main;
	protected VerticalLayout tablePanel;
	protected ComponentWithToolbar tableWithToolbar;
	
	
	
	@Autowired
	public TokensComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, AttributesManagement attrMan)
	{
		
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.attrMan = attrMan;
		this.attrProcessor = attrProcessor;
		initUI();
	}
	
	private void initUI()
	{
		setCaption(msg.getMessage("TokensComponent.caption"));
		
		
		viewer = new TokenViewer(msg,attrProcessor, attrMan);
		viewer.setInput(null);
		table = new GenericElementsTable<Token>(
				msg.getMessage("TokensComponent.tokenTable"),
				new GenericElementsTable.NameProvider<Token>()
				{
					@Override
					public Label toRepresentation(
							Token element)
					{
						return new Label(element.getValue());
					}
				});
		
		table.setMultiSelect(true);
		
		table.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<Token> items = getItems(table
						.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}
				Token item = items.iterator().next();
				viewer.setInput(item);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.setSizeFull();
		
		tablePanel = new VerticalLayout();
		tablePanel.addComponent(table);
		
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		tableWithToolbar = new ComponentWithToolbar(tablePanel, toolbar);
		tableWithToolbar.setSizeFull();
		
		main = new VerticalLayout();
		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, tableWithToolbar, viewer, 25);
		main.addComponent(hl);
	
		refresh();
		
	}
		
	protected Collection<Token> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<Token> items = new ArrayList<Token>();
		for (Object o : c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((Token) i.getElement());
		}
		return items;
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("TokensComponent.refreshAction"),
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
			super(msg.getMessage("TokensComponent.deleteAction"),
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<Token> items = getItems(target);
			

			new ConfirmDialog(msg, msg.getMessage(
					"TokensComponent.confirmDelete"),new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (Token item : items)
					{
						removeToken(item.getType(), item.getValue());
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
					msg.getMessage("TokensComponent.errorRemove"),
					e);
			return false;
		}
	}

	
	protected List<Token> getTokens() throws IllegalIdentityValueException, IllegalTypeException, AuthorizationException
	{
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getAllTokens(INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getAllTokens(INTERNAL_REFRESH_TOKEN));
		return tokens;
	}
	
	protected void refresh()
	{
		try
		{
			table.setInput(getTokens());
			viewer.setInput(null);
			table.setValue(null);
			removeAllComponents();
			addComponent(main);
			
		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg,
					msg.getMessage("TokensComponent.getToken"),
					e);
		}
		
	}
}
