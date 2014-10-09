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

import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;


/**
 * Allows for choosing the language
 * @author K. Benedyczak
 */
@SuppressWarnings("serial")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocaleChoiceComponent extends FormLayout
{
	private ComboBox chooser;
	private Map<String, Locale> selectableLocales;
	
	@Autowired
	public LocaleChoiceComponent(UnityServerConfiguration cfg, UnityMessageSource msg)
	{
		selectableLocales = cfg.getEnabledLocales();
		if (selectableLocales.size() < 2)
		{
			return;
		} else
		{
			chooser = new ComboBox(msg.getMessage("LanguageChoiceComponent.language"));
			String selected = null;
			Locale selectedLocale = InvocationContext.getCurrent().getLocale();
			for (String locale: selectableLocales.keySet())
			{
				chooser.addItem(locale);
				if (selectableLocales.get(locale).equals(selectedLocale))
					selected = locale;
			}
			if (selected != null)
				chooser.select(selected);
			chooser.setTextInputAllowed(false);
			chooser.setNullSelectionAllowed(false);
			chooser.setImmediate(true);
			chooser.addValueChangeListener(new Property.ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					String localeName = (String) chooser.getValue();
					Locale l = selectableLocales.get(localeName);
					
					Cookie languageCookie = new Cookie(UnityVaadinServlet.LANGUAGE_COOKIE, 
							l.toString());
					languageCookie.setPath("/");
					languageCookie.setMaxAge(3600*24*31);
					languageCookie.setHttpOnly(true);
					((VaadinServletResponse)VaadinService.getCurrentResponse()).addCookie(languageCookie);
					
					VaadinSession vSession = VaadinSession.getCurrent();
					VaadinService.getCurrent().closeSession(vSession);
					Page.getCurrent().reload();
				}
			});
			addComponent(chooser);
			setComponentAlignment(chooser, Alignment.TOP_RIGHT);
		}
		setSizeUndefined();
	}
}
