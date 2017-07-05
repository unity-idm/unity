/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

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
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Allows for viewing and removing tokens by the regular user. Changes tables toolbar buttons to standard button (delete,remove) 
 * @author P.Piernik
 *
 */
public class UserHomeTokensComponent extends AdminTokensComponent
{
	private Button removeButton;
	private Button refreshButton;

	public UserHomeTokensComponent(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, AttributesManagement attrMan)
	{
		super(tokenMan, msg, attrProcessor, attrMan);
		setCaption(msg.getMessage("OAuthTokenUserHomeUI.tokensLabel"));
	
		HorizontalLayout buttons = new HorizontalLayout();
		removeButton = new Button(msg.getMessage("OAuthTokenUserHomeUI.remove"));
		removeButton.setIcon(Images.delete.getResource());
		removeButton.setEnabled(false);
		
		refreshButton = new Button(msg.getMessage("OAuthTokenUserHomeUI.refresh"));
		refreshButton.setIcon(Images.refresh.getResource());
		
		buttons.addComponents(refreshButton, removeButton);
		buttons.setSpacing(true);
		buttons.setMargin(new MarginInfo(false, false, true, false));
		
		tokensTablePanel.addComponent(buttons, 0);
		tableWithToolbar.setToolbarVisible(false);
	
		tokensTable.addValueChangeListener(new ValueChangeListener()
	        {

	            @Override
	            public void valueChange(ValueChangeEvent event)
	            {
	                Collection<?> items = (Collection<?>) tokensTable.getValue();
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
				
				final Collection<?> items = (Collection<?>) tokensTable.getValue();
				

				new ConfirmDialog(msg, msg.getMessage(
						"OAuthTokenUserHomeUI.confirmDelete"),new ConfirmDialog.Callback()
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
		});
		tokensTable.setVisibleColumns(new Object[] {"type", "value", "createTime", "expires","refreshToken", "hasIdToken"});	
	}
	
	
	@Override
	protected List<Token> getTokens() throws IllegalIdentityValueException,
			IllegalTypeException, AuthorizationException
	{
		//Get only owned tokens	
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getOwnedTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getOwnedTokens(OAuthProcessor.INTERNAL_REFRESH_TOKEN));
		return tokens;
	}

	
}
