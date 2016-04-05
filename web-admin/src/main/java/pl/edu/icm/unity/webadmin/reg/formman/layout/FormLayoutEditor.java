/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Editor of {@link FormLayout}. Allows for selecting whether the default layout should be used or not.
 * If default layout is not used then list of entries is displayed.
 * The editor allows for incremental updating of the current layout with changes in the upstream form: 
 * all required parameter fields are added and all non existing parameter fields are removed.
 * 
 * @author K. Benedyczak
 */
public class FormLayoutEditor extends CustomComponent
{
	protected UnityMessageSource msg;
	
	protected List<EntryComponent> entries;

	private Component layoutControls;

	private CheckBox enableCustom;
	
	private FormProvider formProvider;

	private VerticalLayout entriesLayout;
	
	private ErrorComponent errorInfo;

	private VerticalLayout main;

	public FormLayoutEditor(UnityMessageSource msg, FormProvider formProvider)
	{
		this.msg = msg;
		this.formProvider = formProvider;
		this.entries = new ArrayList<>();
		initUI();
	}
	
	private void initUI()
	{
		errorInfo = new ErrorComponent();
		errorInfo.setWarning(msg.getMessage("FormLayoutEditor.invalidFormInfo"));
		
		main = new VerticalLayout();
		main.setSpacing(true);
		enableCustom = new CheckBox(msg.getMessage("FormLayoutEditor.enableCustom"));
		enableCustom.addValueChangeListener(event -> {
			boolean enabled = enableCustom.getValue();
			layoutControls.setVisible(enabled);
			if (enabled)
			{
				BaseForm form = getForm();
				if (form != null)
					setLayout(form.getDefaultFormLayout(msg));
			}
		});
		main.addComponent(enableCustom);
		
		layoutControls = getLayoutControls();
		layoutControls.setVisible(false);
		main.addComponent(layoutControls);
		
		setCompositionRoot(main);
	}
	
	private Component getLayoutControls()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		
		main.addComponent(getAddMetaElementControl());
		main.addComponent(HtmlTag.hr());
		
		entriesLayout = new VerticalLayout();
		entriesLayout.setSpacing(true);
		main.addComponent(entriesLayout);
		return main;
	}

	private Component getAddMetaElementControl()
	{
		HorizontalLayout addElementLayout = new HorizontalLayout();
		addElementLayout.setSpacing(true);
		
		Label label = new Label(msg.getMessage("FormLayoutEditor.addCaption"));
		addElementLayout.addComponent(label);
		addElementLayout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		ComboBox elementSelector = new ComboBox();
		elementSelector.addItem(FormLayout.SEPARATOR);
		elementSelector.addItem(FormLayout.CAPTION);
		elementSelector.setNullSelectionAllowed(false);
		elementSelector.setValue(FormLayout.CAPTION);
		
		Button add = new Button();
		add.setIcon(Images.add.getResource());
		add.addClickListener(event -> {
			FormElement added = createExtraElementOfType(elementSelector.getValue().toString());
			addComponentFor(added, 0);
			refreshComponents();
		});
		addElementLayout.addComponents(elementSelector, add);
		return addElementLayout;
	}
	
	public void setInitialForm(BaseForm form)
	{
		if (form != null)
		{
			boolean customLayout = form.getLayout() != null;
			layoutControls.setVisible(customLayout);
			enableCustom.setValue(customLayout);
			if (customLayout)
				setLayout(form.getLayout());
		}
	}

	public void updateFromForm()
	{
		if (!enableCustom.getValue())
			return;

		BaseForm form = getForm();
		if (form != null)
		{
			form.setLayout(getCurrentLayout());
			form.updateLayout();
			setLayout(form.getLayout());
		}
	}
	
	private void setLayout(FormLayout formLayout)
	{
		entries.clear();
		entriesLayout.removeAllComponents();
		for (int i = 0; i < formLayout.getElements().size(); i++)
		{
			FormElement formElement = formLayout.getElements().get(i);
			addComponentFor(formElement, i);
		}
		refreshComponents();
	}
	
	protected void addComponentFor(FormElement formElement, int index)
	{
		FormElementEditor<?> elementEditor = getElementEditor(formElement);
		EntryComponent component = new EntryComponent(index, 
				msg, elementEditor, new CallbackImpl());
		entries.add(index, component);
		entriesLayout.addComponent(component, index);
	}

	protected FormElement createExtraElementOfType(String type)
	{
		switch (type)
		{
		case FormLayout.CAPTION:
			return new FormCaptionElement(new I18nString());
		case FormLayout.SEPARATOR:
			return new FormSeparatorElement();
		default: 
			throw new IllegalStateException("Unsupported extra layout element type " + type);
		}
	}
	
	protected FormElementEditor<?> getElementEditor(FormElement formElement)
	{
		switch (formElement.getType())
		{
		case FormLayout.CAPTION:
			return new CaptionElementEditor(msg, (FormCaptionElement) formElement);
		case FormLayout.AGREEMENT:
		case FormLayout.ATTRIBUTE:
		case FormLayout.CREDENTIAL:
		case FormLayout.GROUP:
		case FormLayout.IDENTITY:
			return new FormParameterElementEditor((FormParameterElement) formElement);
			
		default: 
			return new DefaultElementEditor(formElement);
		}
	}
	
	private FormLayout getCurrentLayout()
	{
		if (!enableCustom.getValue())
			return null;
		return new FormLayout(entries.stream()
				.map(entry -> entry.getEditor().getElement())
				.collect(Collectors.toList()));
	}
	
	public FormLayout getLayout()
	{
		updateFromForm();
		return getCurrentLayout();
	}
	
	private void refreshComponents()
	{
		for (int i=0; i<entries.size(); i++)
			entries.get(i).setPosition(i, entries.size());
	}
	
	private BaseForm getForm()
	{
		BaseForm form = formProvider.getForm();
		if (form == null)
			setCompositionRoot(errorInfo);
		else
			setCompositionRoot(main);
		return form;
	}
	
	private class CallbackImpl implements EntryComponent.Callback
	{
		@Override
		public void moveTo(int oldPosition, int newPosition)
		{
			EntryComponent moved = entries.get(oldPosition);
			EntryComponent oldAtTarget = entries.get(newPosition); 
			entries.set(oldPosition, oldAtTarget);
			entries.set(newPosition, moved);
			entriesLayout.replaceComponent(moved, oldAtTarget);
			refreshComponents();
		}

		@Override
		public void remove(int oldPosition)
		{
			EntryComponent removed = entries.remove(oldPosition);
			entriesLayout.removeComponent(removed);
			refreshComponents();
		}
	}
	
	/**
	 * Used to provide a current form to the layout editor. It is needed
	 * to keep track of all mandatory layout elements and to build default layout.
	 * @author K. Benedyczak
	 */
	public interface FormProvider
	{
		BaseForm getForm();
	}
}
