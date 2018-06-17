/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.tile;

import java.util.List;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

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
	private SelectedAuthNPanel selectedAuthNPanel;
	
	public AuthNTiles(UnityMessageSource msg, List<AuthNTile> tiles, 
			SelectedAuthNPanel selectedAuthNPanel)
	{
		this.msg = msg;
		this.tiles = tiles;
		this.selectedAuthNPanel = selectedAuthNPanel;
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
		main.setSpacing(false);
		main.setMargin(false);
		
		CssLayout tilesL = new CssLayout();
		tilesL.setWidth(100, Unit.PERCENTAGE);
		
		int optionsNum = 0;
		for (int i=0; i<tiles.size(); i++)
		{
			AuthNTile tile = tiles.get(i);
			Component tileComponent = tile.getComponent();
			optionsNum += tile.size();
			tileComponent.addStyleName(Styles.bottomMargin.toString());
			tileComponent.addStyleName("u-authNtile-" + (i+1));
			if (i < tiles.size() - 1)
				tileComponent.addStyleName(Styles.rightMargin.toString());
			tilesL.addComponent(tileComponent);
		}
		
		if (optionsNum >= SHOW_SEARCH_FROM)
		{
			HorizontalLayout wrapper = new HorizontalLayout();
			wrapper.setMargin(new MarginInfo(false, false, true, false));
			wrapper.addStyleName(Styles.verticalPaddingSmall.toString());
			Label info = new Label(msg.getMessage("IdpSelectorComponent.search"));
			TextField search = createSearchField();
			wrapper.addComponents(info, search);
			wrapper.setComponentAlignment(info, Alignment.MIDDLE_RIGHT);
			wrapper.setComponentAlignment(search, Alignment.MIDDLE_LEFT);
			main.addComponent(wrapper);
			main.setComponentAlignment(wrapper, Alignment.MIDDLE_RIGHT);
		} else
		{
			main.addComponent(HtmlTag.br());
		}
		
		main.addComponent(tilesL);
		
		setCompositionRoot(main);
	}

	
	private TextField createSearchField()
	{
		TextField search = new TextField();
		search.addStyleName(Styles.vSmall.toString());
		search.addValueChangeListener(event -> 
		{
			for (AuthNTile tile: tiles)
				tile.filter(event.getValue());
		});
		search.addFocusListener(event -> selectedAuthNPanel.removeEnterKeyBinding());
		search.addBlurListener(event ->	selectedAuthNPanel.restoreEnterKeyBinding());
		return search;
	}	
	
	public List<AuthNTile> getTiles()
	{
		return tiles;
	}
}
