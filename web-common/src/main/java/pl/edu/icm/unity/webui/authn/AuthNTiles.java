/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.List;

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Presents multiple {@link AuthNTile}s with additional search.
 * 
 * @author K. Benedyczak
 */
public class AuthNTiles extends CustomComponent
{
	private UnityMessageSource msg;
	private List<AuthNTile> tiles;

	public AuthNTiles(UnityMessageSource msg, List<AuthNTile> tiles)
	{
		this.msg = msg;
		this.tiles = tiles;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		setCompositionRoot(main);
		
		HorizontalLayout tilesL = new HorizontalLayout();
		tilesL.setSpacing(true);
		
		int optionsNum = 0;
		for (AuthNTile tile: tiles)
		{
			optionsNum += tile.size();
			tilesL.addComponent(tile);
		}
		
		if (optionsNum > 8)
		{
			FormLayout wrapper = new FormLayout();
			wrapper.setMargin(false);
			TextField search = new TextField(msg.getMessage("IdpSelectorComponent.search"));
			search.addStyleName(Styles.vTextfieldSmall.toString());
			search.setImmediate(true);
			wrapper.addComponent(search);
			main.addComponent(wrapper);
			search.addTextChangeListener(new TextChangeListener()
			{
				@Override
				public void textChange(TextChangeEvent event)
				{
					for (AuthNTile tile: tiles)
						tile.filter(event.getText());
				}
			});
		}
		
		main.addComponent(tilesL);
		
		setCompositionRoot(main);
	}

	public List<AuthNTile> getTiles()
	{
		return tiles;
	}
	
	public AuthenticationOption getAuthenticationOptionById(String lastIdp)
	{
		for (AuthNTile tile: tiles)
		{
			AuthenticationOption ret = tile.getAuthenticationOptionById(lastIdp);
			if (ret != null)
				return ret;
		}
		return null;
	}

	public VaadinAuthenticationUI getAuthenticatorById(String lastIdp)
	{
		for (AuthNTile tile: tiles)
		{
			VaadinAuthenticationUI ret = tile.getAuthenticatorById(lastIdp);
			if (ret != null)
				return ret;
		}
		return null;
	}
	
}
