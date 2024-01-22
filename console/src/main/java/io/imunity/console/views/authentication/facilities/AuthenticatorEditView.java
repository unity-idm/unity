/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactoriesRegistry;
import io.imunity.vaadin.elements.*;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

@PermitAll
@Route(value = "/facilities/authenticator", layout = ConsoleMenu.class)
public class AuthenticatorEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticatorsController authenticatorsController;
	private final AuthenticatorEditorFactoriesRegistry editorsRegistry;
	private final NotificationPresenter notificationPresenter;

	private Collection<AuthenticatorTypeDescription> autnTypes;
	private AuthenticatorEditor editor;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;
	private VerticalLayout mainView;
	private VerticalLayout unsavedInfoBanner;

	private VerticalLayout layout;
	private ComboBox<AuthenticatorTypeDescription> authenticatorTypeCombo;
	private Component editorComponent;

	AuthenticatorEditView(MessageSource msg, AuthenticatorsController authenticatorsController,
			AuthenticatorEditorFactoriesRegistry editorsRegistry, NotificationPresenter notificationPresenter,
			AuthenticatorManagement authnMan)
	{
		this.msg = msg;
		this.authenticatorsController = authenticatorsController;
		this.editorsRegistry = editorsRegistry;
		this.notificationPresenter = notificationPresenter;
		this.autnTypes = authnMan.getAvailableAuthenticatorsTypes();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String id)
	{
		getContent().removeAll();

		AuthenticatorEntry entry;
		if(id == null)
		{
			entry = new AuthenticatorEntry(new AuthenticatorDefinition("", "", "", ""), List.of());
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		}
		else
		{
			entry = authenticatorsController.getAuthenticator(id);
			breadCrumbParameter = new BreadCrumbParameter(id, id);
			edit = true;
		}
		initUI(entry);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(AuthenticatorEntry entry)
	{
		Map<AuthenticatorTypeDescription, String> authnTypesSorted = getAuthenticatorTypes();

		authenticatorTypeCombo = new NotEmptyComboBox<>();
		authenticatorTypeCombo.addValueChangeListener(e -> reloadEditor(entry));

		authenticatorTypeCombo.setItemLabelGenerator(authnTypesSorted::get);
		authenticatorTypeCombo.setWidth(TEXT_FIELD_BIG.value());
		authenticatorTypeCombo.setItems(authnTypesSorted.keySet().stream()
				.filter(item -> editorsRegistry.containByName(item.getVerificationMethod()))
				.toList());

		TextField authenticatorTypeLabel = new TextField();
		authenticatorTypeLabel.setWidth(TEXT_FIELD_MEDIUM.value());
		authenticatorTypeLabel.setReadOnly(true);

		FormLayout typeWrapper = new FormLayout();
		typeWrapper.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		typeWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		typeWrapper.addFormItem(authenticatorTypeCombo, msg.getMessage("MainAuthenticatorEditor.typeComboCaption"));
		typeWrapper.addFormItem(authenticatorTypeLabel, msg.getMessage("MainAuthenticatorEditor.typeLabelCaption"));
		layout = new VerticalLayout(typeWrapper);
		layout.setPadding(false);

		if (edit)
		{
			AuthenticatorTypeDescription desc = authnTypesSorted.keySet().stream()
					.filter(t -> t.getVerificationMethod().equals(entry.authenticator().type))
					.findFirst().orElse(null);

			authenticatorTypeCombo.setValue(desc);
			authenticatorTypeCombo.getParent().get().setVisible(false);
			authenticatorTypeLabel.setValue(desc != null ? AuthenticatorTypeLabelHelper.getAuthenticatorTypeLabel(msg, desc) : "");
			authenticatorTypeLabel.getParent().get().setVisible(true);
		} else
		{
			authenticatorTypeCombo.getParent().get().setVisible(true);
			authenticatorTypeLabel.getParent().get().setVisible(false);
			authenticatorTypeCombo.setValue(authnTypesSorted.keySet().iterator().next());
		}
		unsavedInfoBanner = new VerticalLayout(new H4(msg.getMessage("ViewWithSubViewBase.unsavedEdits")));
		unsavedInfoBanner.setWidthFull();
		unsavedInfoBanner.setAlignItems(FlexComponent.Alignment.CENTER);
		unsavedInfoBanner.addClassName("u-unsaved-banner");
		unsavedInfoBanner.setVisible(false);
		mainView = new VerticalLayout(layout,
				createActionLayout(msg, edit, FacilitiesView.class, this::onConfirm));
		getContent().add(unsavedInfoBanner);
		getContent().add(mainView);
	}

	private Map<AuthenticatorTypeDescription, String> getAuthenticatorTypes()
	{
		Map<AuthenticatorTypeDescription, String> res = new LinkedHashMap<>();

		for (AuthenticatorTypeDescription type : autnTypes)
		{
			res.put(type, AuthenticatorTypeLabelHelper.getAuthenticatorTypeLabel(msg, type));
		}

		return res.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
	}

	private void reloadEditor(AuthenticatorEntry entry)
	{
		AuthenticatorTypeDescription type = authenticatorTypeCombo.getValue();
		if (layout.getChildren().anyMatch(child -> child.equals(editorComponent)))
			layout.remove(editorComponent);

		try
		{
			editor = editorsRegistry.getByName(type.getVerificationMethod()).createInstance();
			editorComponent = editor.getEditor(entry.authenticator(), createSubViewSwitcher(), !edit);
			layout.add(editorComponent);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("MainAuthenticatorEditor.createSingleAuthenticatorEditorError"), e.getMessage());
		}
	}

	private SubViewSwitcher createSubViewSwitcher()
	{
		UnityViewComponent viewComponent = this;
		BreadCrumbParameter original = breadCrumbParameter;
		return new SubViewSwitcher()
		{
			private UnitySubView lastSubView;
			private Component currentSubView;

			@Override
			public void exitSubView()
			{
				getContent().remove(currentSubView);
				unsavedInfoBanner.setVisible(false);
				if(lastSubView != null)
				{
					getContent().add((Component) lastSubView);
					setBreadcrumb(lastSubView);
					currentSubView = (Component) lastSubView;
					lastSubView = null;
				}
				else
				{
					mainView.setVisible(true);
					breadCrumbParameter = original;
					currentSubView = null;
				}
				ComponentUtil.fireEvent(UI.getCurrent(), new AfterSubNavigationEvent(viewComponent, false));
			}

			@Override
			public void exitSubViewAndShowUpdateInfo()
			{
				exitSubView();
				unsavedInfoBanner.setVisible(true);
			}

			@Override
			public void goToSubView(UnitySubView subview)
			{
				if(currentSubView != null)
				{
					getContent().remove(currentSubView);
					lastSubView = (UnitySubView) currentSubView;
				}
				currentSubView = (Component)subview;
				unsavedInfoBanner.setVisible(false);
				mainView.setVisible(false);
				getContent().add(currentSubView);
				setBreadcrumb(subview);
				ComponentUtil.fireEvent(UI.getCurrent(), new AfterSubNavigationEvent(viewComponent, false));
			}

			private void setBreadcrumb(UnitySubView subview)
			{
				if(subview.getBreadcrumbs().size() == 1)
					breadCrumbParameter = new BreadCrumbParameter(subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(0), null, true);
				else
					breadCrumbParameter = new BreadCrumbParameter(subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(1), true);
			}
		};
	}

	private void onConfirm()
	{
		AuthenticatorDefinition authenticatorDefinition;
		try
		{
			authenticatorDefinition = editor.getAuthenticatorDefinition();
		} catch (FormValidationException e)
		{
			String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
			if(errorMessage == null || errorMessage.isBlank())
				errorMessage = msg.getMessage("Generic.formErrorHint");
			notificationPresenter.showError(msg.getMessage("EditAuthenticatorView.invalidConfiguration"), errorMessage);
			return;
		}

		try
		{
			if(edit)
				authenticatorsController.updateAuthenticator(authenticatorDefinition);
			else
				authenticatorsController.addAuthenticator(authenticatorDefinition);
		}
		catch (ControllerException ex)
		{
			notificationPresenter.showError(ex.getCaption(), ex.getCause().getMessage());
			return;
		}
		UI.getCurrent().navigate(FacilitiesView.class);
	}
}
