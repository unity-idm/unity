/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.*;

public class LocalizedTextFieldDetails extends CustomField<Map<Locale, String>> implements HasValidator<Map<Locale, String>>
{
	private final Map<Locale, LocalizedTextField> fields = new LinkedHashMap<>();
	private final Binder<Map<Locale, String>> binder = new Binder<>();
	private final LocalizedTextField defaultField;
	private Validator<String> validator = (val, context) -> ValidationResult.ok();

	public LocalizedTextFieldDetails(Collection<Locale> enabledLocales, Locale currentLocale)
	{
		this(enabledLocales, currentLocale, null);
	}

	public LocalizedTextFieldDetails(Collection<Locale> enabledLocales, Locale currentLocale, String label)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);
		content.setSpacing(false);

		Icon angleDown = crateIcon(VaadinIcon.ANGLE_DOWN, label);
		angleDown.getStyle().set("margin-top", BASE_MARGIN.value());
		Icon angleUp = crateIcon(VaadinIcon.ANGLE_UP, label);
		angleUp.getStyle().set("margin-top", BASE_MARGIN.value());

		angleUp.setVisible(false);
		angleDown.addClickListener(event ->
		{
			angleDown.setVisible(false);
			angleUp.setVisible(true);
			content.setVisible(true);
		});
		angleUp.addClickListener(event ->
		{
			angleDown.setVisible(true);
			angleUp.setVisible(false);
			content.setVisible(false);
		});

		defaultField = new LocalizedTextField(currentLocale);
		defaultField.setLabel(label);
		fields.put(currentLocale, defaultField);

		HorizontalLayout summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setWidthFull();
		summary.setClassName(SMALL_GAP.getName());

		enabledLocales.stream()
				.filter(locale -> !currentLocale.equals(locale))
				.forEach(locale ->
				{
					LocalizedTextField localizedTextField = new LocalizedTextField(locale);
					content.add(localizedTextField);
					fields.put(locale, localizedTextField);
				});
		fields.forEach((locale, field) ->
				binder.forField(field)
				.withValidator((val, context) -> validator.apply(val, context))
				.bind(map -> map.get(locale), (map, val) -> map.put(locale, val))
		);

		setValue(new LinkedHashMap<>());
		add(summary, content);
		propagateValueChangeEventFromNestedTextFieldToThisComponent();
	}

	private void propagateValueChangeEventFromNestedTextFieldToThisComponent()
	{
		fields.values().forEach(localizedTextField -> localizedTextField.addValueChangeListener(event ->
				fireEvent(new ComponentValueChangeEvent<>(this, event.getHasValue(), event.getOldValue(), event.isFromClient()))
		));
	}

	public void addValuesChangeListener(BiConsumer<HasValue<?, String>, Integer> consumer)
	{
		fields.values().forEach(field -> field.getElement().addEventListener(
				"keyup",
				e -> field.getElement().executeJs("return this.inputElement.selectionStart")
						.then(Integer.class, pos -> consumer.accept(field, pos)))
		);

		fields.values().forEach(field -> field.addFocusListener(
				e -> field.getElement().executeJs("return this.inputElement.selectionStart")
						.then(Integer.class, pos -> consumer.accept(field, pos)))
		);
	}

	public void setValidator(Validator<String> validator)
	{
		this.validator = validator;
	}

	public Collection<LocalizedTextField> getSlottedFields()
	{
		return fields.values();
	}

	private Icon crateIcon(VaadinIcon angleDown, String label)
	{
		Icon icon = angleDown.create();
		icon.addClassName(DETAILS_ICON.getName());
		if(label != null)
			icon.setClassName(EMPTY_DETAILS_ICON.getName());
		return icon;
	}

	@Override
	public void setWidthFull()
	{
		super.setWidthFull();
		fields.values().forEach(HasSize::setWidthFull);
	}

	@Override
	public void setWidth(String width)
	{
		fields.values().forEach(f -> f.setWidth(width));
	}

	@Override
	public void focus()
	{
		fields.values().iterator().next().focus();
	}

	@Override
	public void setValue(Map<Locale, String> value)
	{
		binder.setBean(new LinkedHashMap<>(value));
	}

	@Override
	public Map<Locale, String> getValue()
	{
		return binder.getBean();
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		VaadinElementReadOnlySetter.setReadOnly(getElement(), readOnly);
		fields.values().forEach(field -> field.setReadOnly(readOnly));
	}

	@Override
	protected Map<Locale, String> generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(Map<Locale, String> newPresentationValue)
	{
		setValue(newPresentationValue);
	}

	@Override
	public Validator<Map<Locale, String>> getDefaultValidator()
	{
		return (value, context) ->
		{
			if (binder.isValid()) {
				return ValidationResult.ok();
			} else {
				return ValidationResult.error("");
			}
		};
	}

	@Override
	public void setLabel(String label)
	{
		defaultField.setLabel(label);
	}

	@Override
	public String getLabel()
	{
		return defaultField.getLabel();
	}

	public void setPlaceholder(String message)
	{
		defaultField.setPlaceholder(message);
		
	}
	
}
