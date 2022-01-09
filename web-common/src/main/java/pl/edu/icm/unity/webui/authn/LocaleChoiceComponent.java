/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Images;


/**
 * Allows for choosing the language
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocaleChoiceComponent extends CompactFormLayout
{
	private MenuBar chooser;
	private Map<String, Locale> selectableLocales;
	
	@Autowired
	public LocaleChoiceComponent(UnityServerConfiguration cfg, MessageSource msg)
	{
		selectableLocales = cfg.getEnabledLocales();
		if (selectableLocales.size() < 2)
		{
			return;
		} else
		{
			String selected = null;
			Locale selectedLocale = InvocationContext.getCurrent().getLocale();
			for (Map.Entry<String, Locale> locale : selectableLocales.entrySet())
			{
				if (locale.getValue().equals(selectedLocale))
					selected = locale.getKey();
			}
			
			chooser = new MenuBar();
			chooser.setCaption(msg.getMessage("LanguageChoiceComponent.language"));
			chooser.addStyleName("u-authn-languageSelector");

			MenuItem current = chooser.addItem(selected, Images.getFlagForLocale(selectableLocales.get(selected.toString()).toString()), null);
			current.setStyleName("u-authn-languageSelector-first");
			
			for (String locale : selectableLocales.keySet())
			{
				current.addItem(locale, Images.getFlagForLocale(selectableLocales.get(locale).toString()), s -> 
				{
					Locale l = selectableLocales.get(locale);
					Cookie languageCookie = new LanguageCookie(l.toString());
					((VaadinServletResponse)VaadinService.getCurrentResponse()).addCookie(languageCookie);
					
					VaadinSession vSession = VaadinSession.getCurrent();
					VaadinService.getCurrent().closeSession(vSession);
					Page.getCurrent().reload();		
				});
			}
			
			addComponent(chooser);
			setComponentAlignment(chooser, Alignment.TOP_RIGHT);
		}
		setSizeUndefined();
	}
}