/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletResponse;

import io.imunity.vaadin.elements.FlagIcon;
import jakarta.servlet.http.Cookie;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;


public class LocaleChoiceComponent extends Div
{
	public LocaleChoiceComponent(UnityServerConfiguration cfg)
	{
		Map<String, Locale> selectableLocales = cfg.getEnabledLocales();
		if (selectableLocales.size() >= 2)
		{
			Locale selected = InvocationContext.getCurrent().getLocale();
			Map<Locale, String> collect = selectableLocales.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

			ComboBox<Locale> chooser = new ComboBox<>();
			chooser.addClassName("u-authn-languageSelector");

			chooser.addValueChangeListener(event ->
			{
				if(!event.isFromClient())
				{
					return;
				}
				Locale l = event.getValue();
				Cookie languageCookie = new LanguageCookie(l.toString());
				((VaadinServletResponse)VaadinService.getCurrentResponse()).addCookie(languageCookie);
				SessionStorage.consumeRedirectUrl((redirectUrl, currentRelativeURI) ->
				{
					UI.getCurrent().getPage().setLocation(redirectUrl);
				});
			});
			chooser.setRenderer(new ComponentRenderer<>(
					locale -> new Span(new FlagIcon(locale.getLanguage()), getLabel(collect, locale)))
			);
			chooser.setItemLabelGenerator(collect::get);
			chooser.setItems(selectableLocales.values());
			chooser.setValue(selected);
			setPrefixComponent(chooser, new FlagIcon(selected.getLanguage()));

			add(chooser);
		}
	}

	private static Span getLabel(Map<Locale, String> collect, Locale locale)
	{
		Span label = new Span(collect.get(locale));
		label.getStyle().set("margin-left", "0.3em");
		return label;
	}

	void setPrefixComponent(Component main, Component component) {
		SlotUtils.clearSlot(main, "prefix");

		if (component != null) {
			component.getElement().setAttribute("slot", "prefix");
			main.getElement().appendChild(component.getElement());
		}
	}

}