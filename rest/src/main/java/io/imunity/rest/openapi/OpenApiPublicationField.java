package io.imunity.rest.openapi;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.rest.RESTEndpointProperties.OpenApiPublication;

public final class OpenApiPublicationField extends FormLayout
{
	public <T> OpenApiPublicationField(MessageSource msg, Binder<T> binder,
			ValueProvider<T, OpenApiPublication> getter, Setter<T, OpenApiPublication> setter)
	{
		setResponsiveSteps(new ResponsiveStep("0", 1));
		addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		ComboBox<OpenApiPublication> publication = new ComboBox<>();
		publication.getElement().setAttribute("data-testid", "OpenApiPublicationField.publication");
		publication.setWidth(TEXT_FIELD_BIG.value());
		publication.setItems(OpenApiPublication.values());
		publication.setItemLabelGenerator(value -> msg.getMessage("OpenApiPublicationField." + value.name()));
		publication.setHelperText(msg.getMessage("OpenApiPublicationField.help"));
		binder.forField(publication).asRequired(msg.getMessage("fieldRequired")).bind(getter, setter);
		addFormItem(publication, msg.getMessage("OpenApiPublicationField.label"));
	}
}
