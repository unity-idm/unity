/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.registration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import io.imunity.vaadin23.elements.FlagIcon;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.LanguageCookie;

import javax.servlet.http.Cookie;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


public class LocaleChoiceComponent extends Div
{
	public LocaleChoiceComponent(UnityServerConfiguration cfg, MessageSource msg)
	{
		Map<String, Locale> selectableLocales = cfg.getEnabledLocales();
		if (selectableLocales.size() >= 2)
		{
			Locale selected = InvocationContext.getCurrent().getLocale();
			Map<Locale, String> collect = selectableLocales.entrySet().stream().collect(Collectors.toMap(x -> x.getValue(), x -> x.getKey()));

			ComboBox<Locale> chooser = new ComboBox<>();
			chooser.setLabel(msg.getMessage("LanguageChoiceComponent.language"));
			chooser.addClassName("u-authn-languageSelector");

			chooser.addValueChangeListener(event ->
			{
				if(!event.isFromClient())
					return;
				Locale l = event.getValue();
				Cookie languageCookie = new LanguageCookie(l.toString());
				((VaadinServletResponse)VaadinService.getCurrentResponse()).addCookie(languageCookie);

				VaadinSession vSession = VaadinSession.getCurrent();
				VaadinService.getCurrent().closeSession(vSession);
				UI.getCurrent().getPage().reload();
			});
			chooser.setRenderer(new ComponentRenderer<>(
					locale -> new Span(new FlagIcon(locale.getLanguage()), new Label(collect.get(locale))))
			);
			chooser.setItemLabelGenerator(collect::get);
			chooser.setItems(selectableLocales.values());
			chooser.setValue(selected);
			setPrefixComponent(chooser, new FlagIcon(selected.getLanguage()));

			add(chooser);
		}
	}

	void setPrefixComponent(Component main, Component component) {
		SlotUtils.clearSlot(main, "prefix");

		if (component != null) {
			component.getElement().setAttribute("slot", "prefix");
			main.getElement().appendChild(component.getElement());
		}
	}

}