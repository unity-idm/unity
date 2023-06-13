/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.server.ErrorMessage;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Base class for implementations of custom fields allowing for editing an
 * {@link I18nString}, i.e. a string in several languages. By default the
 * default locale is only shown, but a special buttons can be clicked to add or
 * remove text fields with another translations.
 * <p>
 * Extensions can define what component is used for editing values,
 * {@link TextField} or {@link RichTextArea}.
 * 
 * @author P.Piernik
 */
abstract class AbstractOptionalLang18nField<T extends AbstractField<String>> extends CustomField<I18nString>
{
	private MessageSource msg;
	private String defaultLocaleCode;
	private Map<String, String> enabledLocalesNamesByCode;
	private SingleLanguageEditBox defaultTf;
	private List<SingleLanguageEditBox> translationTFs;
	private Map<String, String> notShownTranslations = new HashMap<>();
	private String preservedDef;
	private VerticalLayout main;

	public AbstractOptionalLang18nField(MessageSource msg)
	{
		this.enabledLocalesNamesByCode = msg.getEnabledLocales().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getValue().toString(), e -> e.getKey()));
		this.defaultLocaleCode = msg.getDefaultLocaleCode();
		this.msg = msg;

	}

	public AbstractOptionalLang18nField(MessageSource msg, String caption)
	{
		this(msg);
		setCaption(caption);
	}

	protected abstract T makeFieldInstance();

	protected void initUI()
	{
		main = new VerticalLayout();
		main.setMargin(false);
		init();
	}

	private void init()
	{
		translationTFs = new ArrayList<>();
		defaultTf = new SingleLanguageEditBox();
		defaultTf.setLang(defaultLocaleCode);
		defaultTf.setLangReadOnly(true);
		defaultTf.setRemoveVisiable(false);
		translationTFs.add(defaultTf);
		main.addComponent(defaultTf);
	}

	private SingleLanguageEditBox add()
	{
		SingleLanguageEditBox field = new SingleLanguageEditBox();
		translationTFs.add(field);
		main.addComponent(field);
		refreshLangAndButtons();
		return field;
	}

	public void remove(SingleLanguageEditBox languageBox)
	{
		translationTFs.remove(languageBox);
		main.removeComponent(languageBox);
		refreshLangAndButtons();
	}

	public void refreshLangAndButtons()
	{
		translationTFs.get(0).setRemoveVisiable(false);
		translationTFs.stream().forEach(b -> b.setAddVisiable(false));

		if (!translationTFs.stream().map(l -> l.getLang()).collect(Collectors.toSet())
				.containsAll(enabledLocalesNamesByCode.keySet()))
		{
			translationTFs.get(translationTFs.size() - 1).setAddVisiable(true);
		}
		translationTFs.stream().forEach(b -> b.refreshLang());
		fireEvent(new ValueChangeEvent<I18nString>(this, getValue(), true));

	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		defaultTf.setComponentError(componentError);
		translationTFs.stream().forEach(b -> b.setComponentError(componentError));
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	public I18nString getEmptyValue()
	{

		return new I18nString();
	}

	@Override
	public I18nString getValue()
	{
		I18nString ret = new I18nString();
		if (defaultTf.getValue() != null && !defaultTf.getValue().equals(""))
			ret.addValue(defaultLocaleCode, defaultTf.getValue());
		for (SingleLanguageEditBox tfE : translationTFs)
		{
			if (tfE.getValue() != null && !tfE.getValue().equals(""))
				ret.addValue(tfE.getLang(), tfE.getValue());
		}
		for (Map.Entry<String, String> hiddenE : notShownTranslations.entrySet())
			ret.addValue(hiddenE.getKey(), hiddenE.getValue());
		ret.setDefaultValue(preservedDef);
		return ret;
	}

	@Override
	protected void doSetValue(I18nString value)
	{
		main.removeAllComponents();
		init();

		if (value == null)
			return;

		for (Map.Entry<String, String> vE : value.getMap().entrySet())
		{
			if (vE.getKey().equals(defaultLocaleCode))
				defaultTf.setValue(vE.getValue());
			else if (enabledLocalesNamesByCode.containsKey(vE.getKey()))
			{
				SingleLanguageEditBox toAdd = new SingleLanguageEditBox();
				toAdd.setValue(vE.getValue());
				toAdd.setLang(vE.getKey());
				translationTFs.add(toAdd);
				main.addComponent(toAdd);
			} else
			{
				notShownTranslations.put(vE.getKey(), vE.getValue());

			}
		}
		preservedDef = value.getDefaultValue();
		refreshLangAndButtons();
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		super.setWidth(width, unit);
		if (translationTFs != null)
		{
			main.setWidth(width, unit);
			defaultTf.setWidth(100, Unit.PERCENTAGE);
			for (SingleLanguageEditBox tf : translationTFs)
			{
				tf.setWidth(width, unit);
			}
		}
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		defaultTf.setReadOnly(readOnly);
		translationTFs.forEach(tf -> tf.setReadOnly(readOnly));
	}

	public void fireChange()
	{
		fireEvent(new ValueChangeEvent<I18nString>(this, getValue(), true));
	}

	public class SingleLanguageEditBox extends CustomComponent
	{
		private Button remove;
		private Button add;
		private ComboBox<String> lang;
		private T field;

		public SingleLanguageEditBox()
		{
			this.field = makeFieldInstance();
			this.field.addValueChangeListener(e -> fireChange());
			init();
		}

		private void init()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);

			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);

			add = new Button();
			add.setIcon(Images.add.getResource());
			add.setDescription(msg.getMessage("add"));
			add.addStyleName(Styles.toolbarButton.toString());
			add.addStyleName(Styles.vButtonLink.toString());
			add.addClickListener(e -> add());
			buttons.addComponent(add);

			remove = new Button();
			remove.setIcon(Images.delete.getResource());
			remove.setDescription(msg.getMessage("remove"));
			remove.addStyleName(Styles.toolbarButton.toString());
			remove.addStyleName(Styles.vButtonLink.toString());
			remove.addClickListener(e -> remove(this));
			buttons.addComponent(remove);

			lang = new ComboBox<>();
			lang.setItems(enabledLocalesNamesByCode.keySet().stream().filter(l -> !translationTFs.stream()
					.filter(b -> b.getLang().equals(l)).findAny().isPresent()));
			lang.setEmptySelectionAllowed(false);
			lang.setValue(enabledLocalesNamesByCode
					.keySet().stream().filter(l -> !translationTFs.stream()
							.filter(b -> b.getLang().equals(l)).findAny().isPresent())
					.findFirst().orElse(null));
			lang.setItemIconGenerator(i -> Images.getFlagForLocale(i));
			lang.setItemCaptionGenerator(i -> enabledLocalesNamesByCode.get(i));
			buttons.addComponent(lang);

			lang.addValueChangeListener(e -> fireChange());

			HorizontalLayout buttonsWrapper = new HorizontalLayout();
			buttonsWrapper.setWidth(100, Unit.PERCENTAGE);
			buttonsWrapper.addComponent(buttons);
			buttonsWrapper.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);

			main.addComponent(buttonsWrapper);
			main.addComponent(field);
			setCompositionRoot(main);
		}

		public void setLangReadOnly(boolean readOnly)
		{
			lang.setReadOnly(readOnly);
			if (readOnly)
			{
				lang.addStyleName(Styles.readOnlyComboBox.toString());
			} else
			{
				lang.removeStyleName(Styles.readOnlyComboBox.toString());
			}
		}

		public void setReadOnly(boolean readOnly)
		{
			lang.setReadOnly(readOnly);
			field.setReadOnly(readOnly);
		}

		public void setValue(String value)
		{
			field.setValue(value);

		}

		public void setAddVisiable(boolean visible)
		{
			add.setVisible(visible);
		}

		public void setRemoveVisiable(boolean visible)
		{
			remove.setVisible(visible);

		}

		public String getLang()
		{
			return lang.getValue();
		}

		public void setLang(String value)
		{
			lang.setValue(value);
		}

		public void refreshLang()
		{
			lang.setItems(enabledLocalesNamesByCode.keySet().stream().filter(l -> !translationTFs.stream()
					.filter(b -> b != this && b.getLang().equals(l)).findAny().isPresent()));
		}

		public String getValue()
		{
			return field.getValue();
		}
		
		@Override
		public void setComponentError(ErrorMessage componentError)
		{
			field.setComponentError(componentError);
		}

	}

}
