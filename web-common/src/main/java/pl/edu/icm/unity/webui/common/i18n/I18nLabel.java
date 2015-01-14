/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows {@link I18nString} in read only mode. Implemented as Custom field for convenience. 
 * @author K. Benedyczak
 */
public class I18nLabel extends CustomField<I18nString>
{
	private String defaultLocaleCode;
	private Map<String, Locale> enabledLocales;
	private Label defaultTf;
	private Map<String, HPairLayout> translationTFs = new HashMap<String, HPairLayout>();
	private Component main;
	
	public I18nLabel(UnityMessageSource msg)
	{
		this.enabledLocales = new HashMap<String, Locale>(msg.getEnabledLocales());
		this.defaultLocaleCode = msg.getDefaultLocaleCode();
		initUI();
	}

	public I18nLabel(UnityMessageSource msg, String caption)
	{
		this(msg);
		setCaption(caption);
	}
	
	private void initUI()
	{
		HPairLayout defL = new HPairLayout();
		defaultTf = new Label();
		Resource defStyle = Images.getFlagForLocale(defaultLocaleCode);
		if (defStyle != null)
			defL.addImage(defStyle);
		defL.addLabel(defaultTf);
		
		VerticalLayout main = new VerticalLayout();
//		main.setSpacing(true);
//		main.addStyleName(Styles.smallSpacing.toString());
		main.addComponent(defL);

		for (Map.Entry<String, Locale> locE: enabledLocales.entrySet())
		{
			String localeKey = locE.getValue().toString();
			if (defaultLocaleCode.equals(localeKey))
				continue;

			HPairLayout pair = new HPairLayout();
			Label tf = new Label();
			pair.addLabel(tf);
			Resource image = Images.getFlagForLocale(localeKey);
			if (image != null)
				pair.addImage(image);
			translationTFs.put(locE.getValue().toString(), pair);
			
			main.addComponent(pair);
		}
		this.main = main;
	}

	
	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	public void setValue(I18nString value)
	{
		super.setValue(value);
		for (HPairLayout locE: translationTFs.values())
			locE.setVisible(false);
		defaultTf.setVisible(false);
		for (Map.Entry<String, String> vE: value.getMap().entrySet())
		{
			if (vE.getKey().equals(defaultLocaleCode))
			{
				defaultTf.setValue(vE.getValue());
				defaultTf.setVisible(true);
			} else
			{
				HPairLayout tf = translationTFs.get(vE.getKey());
				if (tf != null)
				{
					tf.setLabelValue(vE.getValue());
					tf.setVisible(true);
				}
			}
		}
		if (!defaultTf.isVisible() && value.getDefaultValue() != null)
		{
			defaultTf.setValue(value.getDefaultValue());
			defaultTf.setVisible(true);
		}
	}
	
	@Override
	public Class<? extends I18nString> getType()
	{
		return I18nString.class;
	}
	
	private class HPairLayout extends HorizontalLayout
	{
		private Label label;
		
		public HPairLayout()
		{
			setSpacing(true);
			addStyleName(Styles.smallSpacing.toString());
		}
		
		public void addImage(Resource res)
		{
			Image img = new Image();
			img.setSource(res);
			addComponentAsFirst(img);
			setComponentAlignment(img, Alignment.MIDDLE_LEFT);
		}
		
		public void addLabel(Label l)
		{
			addComponent(l);
			setComponentAlignment(l, Alignment.MIDDLE_LEFT);
			this.label = l;
		}
		
		public void setLabelValue(String value)
		{
			this.label.setValue(value);
		}
	}
}
