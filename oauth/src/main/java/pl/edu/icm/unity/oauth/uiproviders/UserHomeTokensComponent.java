/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
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
			EntityManagement entityManagement)
	{
		super(tokenMan, msg, entityManagement, false);
		setCaption("");
		HorizontalLayout buttons = new HorizontalLayout();
		removeButton = new Button(msg.getMessage("OAuthTokenUserHomeUI.remove"));
		removeButton.setIcon(Images.delete.getResource());
		removeButton.setEnabled(false);

		refreshButton = new Button(msg.getMessage("OAuthTokenUserHomeUI.refresh"));
		refreshButton.setIcon(Images.refresh.getResource());

		buttons.addComponents(refreshButton, removeButton);
		buttons.setMargin(new MarginInfo(false, false, true, false));

		tokensTablePanel.addComponent(buttons, 0);
		tableWithToolbar.setToolbarVisible(false);

		tokensTable.addSelectionListener(event ->
		{
			Collection<TableTokensBean> items = event.getAllSelectedItems();
			removeButton.setEnabled(items.size() > 0);
		});

		refreshButton.addClickListener(event -> refresh());

		removeButton.addClickListener(event -> 
		{
			Collection<TableTokensBean> items = tokensTable.getSelectedItems();
			new ConfirmDialog(msg, msg.getMessage("OAuthTokenUserHomeUI.confirmDelete"),
					() -> 
			{
				for (TableTokensBean item : items)
					removeToken(item.getRealType(), item.getValue());
			}
			).show();
		});
		tokensTable.getColumn("owner").setHidden(true);
	}


	@Override
	protected List<Token> getTokens() throws EngineException
	{
		//Get only owned tokens	
		List<Token> tokens = new ArrayList<>();	
		tokens.addAll(tokenMan.getOwnedTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN));
		tokens.addAll(tokenMan.getOwnedTokens(OAuthProcessor.INTERNAL_REFRESH_TOKEN));
		return tokens;
	}
}
