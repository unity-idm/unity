/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.components.LocalizedRichTextEditorDetails;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.files.URIHelper;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;

@PermitAll
@Route(value = "/policy-documents/edit", layout = ConsoleMenu.class)
public class PolicyDocumentEditView extends ConsoleViewComponent
{

	private final MessageSource msg;
	private final PolicyDocumentsController controller;

	private Binder<PolicyDocumentEntry> binder;
	private FormLayout mainLayout;
	private CustomField<Map<Locale, String>> content;
	private FormLayout.FormItem contentItem;
	private Binder.Binding<PolicyDocumentEntry, Map<Locale, String>> contentBind;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;


	PolicyDocumentEditView(MessageSource msg, PolicyDocumentsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String policyDocumentId)
	{
		getContent().removeAll();
		contentItem = null;
		PolicyDocumentEntry policyDocumentEntry;
		if (policyDocumentId == null)
		{
			policyDocumentEntry = new PolicyDocumentEntry();
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		} else
		{
			policyDocumentEntry = controller.getPolicyDocument(Long.parseLong(policyDocumentId));
			breadCrumbParameter = new BreadCrumbParameter(policyDocumentId, policyDocumentEntry.name);
			edit = true;
		}
		Set<String> allNames = controller.getPolicyDocuments().stream().map(d -> d.name)
				.filter(d -> !d.equals(policyDocumentEntry.name)).collect(Collectors.toSet());
		initUI(policyDocumentEntry, allNames);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(PolicyDocumentEntry toEdit, Set<String> allNames)
	{
		TextField name = new TextField();
		name.setPlaceholder(msg.getMessage("PolicyDocumentEditor.defaultName"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(
				msg.getEnabledLocales().values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_BIG.value());

		Checkbox optional = new Checkbox();
		Span revision = new Span(String.valueOf(toEdit.revision));

		ComboBox<PolicyDocumentContentType> type = new ComboBox<>();
		type.setItems(PolicyDocumentContentType.values());
		type.setItemLabelGenerator(item -> msg.getMessage("PolicyDocumentType." + item));
		type.addValueChangeListener(e ->
		{
			setContentType(e.getValue());
			if(e.isFromClient())
				content.setValue(Map.of());
		});

		initFormLayout(name, displayedName, optional, revision, type);
		initBinder(allNames, name, displayedName, optional, type);

		getContent().add(new VerticalLayout(mainLayout,
				createActionLayout(msg, edit, PolicyDocumentsView.class, this::onConfirm)));
		binder.setBean(toEdit);
		content.setValue(toEdit.content);
	}

	private void initFormLayout(TextField name, LocalizedTextFieldDetails displayedName, Checkbox optional, Span revision,
			ComboBox<PolicyDocumentContentType> type)
	{
		mainLayout = new FormLayout();
		mainLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainLayout.addFormItem(name, msg.getMessage("PolicyDocumentEditor.name"));
		mainLayout.addFormItem(displayedName, msg.getMessage("PolicyDocumentEditor.displayedName"));
		HorizontalLayout optionalField = new HorizontalLayout(optional,
				new Span(msg.getMessage("PolicyDocumentEditor.optionalAcceptance")));
		optionalField.setSpacing(false);
		mainLayout.addFormItem(optionalField, "");
		mainLayout.addFormItem(revision, msg.getMessage("PolicyDocumentEditor.revision"));
		mainLayout.addFormItem(type, msg.getMessage("PolicyDocumentEditor.contentType"));
	}

	private void initBinder(Set<String> allNames, TextField name, LocalizedTextFieldDetails displayedName,
			Checkbox optional, ComboBox<PolicyDocumentContentType> type)
	{
		binder = new Binder<>(PolicyDocumentEntry.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator((s, c) ->
				{
					if (allNames.contains(s))
						return ValidationResult.error(msg.getMessage("PolicyDocumentEditor.nameExists"));
					else
						return ValidationResult.ok();
				}).bind(PolicyDocumentEntry::getName, PolicyDocumentEntry::setName);
		binder.forField(displayedName)
				.bind(PolicyDocumentEntry::getDisplayedName, PolicyDocumentEntry::setDisplayedName);
		binder.forField(optional).bind(PolicyDocumentEntry::isOptional, PolicyDocumentEntry::setOptional);
		binder.forField(type).bind(PolicyDocumentEntry::getContentType, PolicyDocumentEntry::setContentType);
	}

	private void onConfirm()
	{
		binder.validate();
		if (binder.isValid())
		{
			PolicyDocumentEntry bean = binder.getBean();
			bean.setContent(content.getValue());
			if (edit)
			{
				new PolicyUpdateConfirmationDialog(
						msg.getMessage("EditPolicyDocumentView.confirm"),
						msg.getMessage("EditPolicyDocumentView.updateInfo"),
						msg.getMessage("EditPolicyDocumentView.saveOfficialUpdate"),
						() ->
						{
							controller.updatePolicyDocument(bean, true);
							UI.getCurrent().navigate(PolicyDocumentsView.class);
						},
						msg.getMessage("EditPolicyDocumentView.saveSilently"),
						() ->
						{
							controller.updatePolicyDocument(bean, false);
							UI.getCurrent().navigate(PolicyDocumentsView.class);
						},
						msg.getMessage("cancel")
				).open();
			} else
			{
				controller.addPolicyDocument(bean);
				UI.getCurrent().navigate(PolicyDocumentsView.class);
			}
		}
	}

	private void setContentType(PolicyDocumentContentType value)
	{
		if (contentItem != null)
			mainLayout.remove(contentItem);
		if (contentBind != null)
			binder.removeBinding(contentBind);

		if (value.equals(PolicyDocumentContentType.EMBEDDED))
		{
			content = new LocalizedRichTextEditorDetails(msg.getEnabledLocales().values(), msg.getLocale(),
					locale -> "");
			contentItem = mainLayout.addFormItem(content, msg.getMessage("PolicyDocumentEditor.text"));
		} else
		{
			LocalizedTextFieldDetails content = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
			contentItem = mainLayout.addFormItem(content, msg.getMessage("PolicyDocumentEditor.url"));
			content.setWidth(TEXT_FIELD_BIG.value());
			content.setValidator((val, context) ->
			{
				if (val != null && !val.isEmpty() && !URIHelper.isWebReady(val))
					return ValidationResult.error(msg.getMessage("FileField.notWebUri"));
				else
					return ValidationResult.ok();
			});
			this.content = content;
		}
		contentBind = binder.forField(content)
				.bind(PolicyDocumentEntry::getContent, PolicyDocumentEntry::setContent);
		contentItem.getStyle().set("align-items", "flex-start");
	}
}
