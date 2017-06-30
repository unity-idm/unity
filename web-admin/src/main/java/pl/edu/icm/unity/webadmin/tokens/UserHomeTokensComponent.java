/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Allows for viewing and removing tokens by the regular user
 * @author P.Piernik
 *
 */
public class UserHomeTokensComponent extends TokensComponent
{
	private Button removeButton;
	private Button refreshButton;

	public UserHomeTokensComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, AttributesManagement attrMan)
	{
		super(tokenMan, msg, attrProcessor, attrMan);
		HorizontalLayout buttons = new HorizontalLayout();
		removeButton = new Button(msg.getMessage("UserTokens.remove"));
		removeButton.setIcon(Images.delete.getResource());
		removeButton.setEnabled(false);
		
		refreshButton = new Button(msg.getMessage("UserTokens.refresh"));
		refreshButton.setIcon(Images.refresh.getResource());
		
		buttons.addComponents(refreshButton, removeButton);
		buttons.setSpacing(true);
		buttons.setMargin(new MarginInfo(false, false, true, false));
		
		tablePanel.addComponent(buttons, 0);
		tableWithToolbar.setToolbarVisible(false);
		table.setValue(null);
		
		table.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<Token> items = getItems(table
						.getValue());
				if (items.size() > 0 )
				{
					removeButton.setEnabled(true);
				}else
				{
					removeButton.setEnabled(false);
				}
				
			}
		});
		
		
		refreshButton.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				
					refresh();
				
			}
		});
		
		removeButton.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				
				final Collection<Token> items = getItems(table.getValue());
				

				new ConfirmDialog(msg, msg.getMessage(
						"UserTokens.confirmDelete"),new ConfirmDialog.Callback()
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
		});
		viewer.setShowId(false);
			
	}

	@Override
	protected List<Token> getTokens() throws IllegalIdentityValueException,
			IllegalTypeException, AuthorizationException
	{
			
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getOwnedTokens(INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getOwnedTokens(INTERNAL_REFRESH_TOKEN));
		return tokens;
	}

	
}
