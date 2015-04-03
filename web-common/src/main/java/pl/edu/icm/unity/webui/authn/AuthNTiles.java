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
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Presents multiple {@link AuthNTile}s with additional search.
 * 
 * @author K. Benedyczak
 */
public class AuthNTiles extends CustomComponent
{
	private static final int SHOW_SEARCH_FROM = 8;
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
		
		Label title = new Label(msg.getMessage("AuthenticationUI.selectMethod"));
		title.addStyleName(Styles.textSubHeading.toString());
		main.addComponent(title);
		main.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
		
		HorizontalLayout tilesL = new HorizontalLayout();
		tilesL.setSpacing(true);
		
		int optionsNum = 0;
		for (AuthNTile tile: tiles)
		{
			optionsNum += tile.size();
			tilesL.addComponent(tile);
		}
		
		if (optionsNum >= SHOW_SEARCH_FROM)
		{
			HorizontalLayout wrapper = new HorizontalLayout();
			wrapper.setMargin(new MarginInfo(false, false, true, false));
			wrapper.addStyleName(Styles.verticalPadding10.toString());
			Label info = new Label(msg.getMessage("IdpSelectorComponent.search"));
			TextField search = new TextField();
			search.addStyleName(Styles.vSmall.toString());
			search.setImmediate(true);
			wrapper.addComponents(info, search);
			wrapper.setComponentAlignment(info, Alignment.MIDDLE_RIGHT);
			wrapper.setComponentAlignment(search, Alignment.MIDDLE_LEFT);
			wrapper.setSpacing(true);
			main.addComponent(wrapper);
			main.setComponentAlignment(wrapper, Alignment.MIDDLE_RIGHT);
			search.addTextChangeListener(new TextChangeListener()
			{
				@Override
				public void textChange(TextChangeEvent event)
				{
					for (AuthNTile tile: tiles)
						tile.filter(event.getText());
				}
			});
		} else
		{
			main.addComponent(HtmlTag.br());
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
