/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.console.SimpleIDPMetaConverter.SAMLEntity;
import pl.edu.icm.unity.saml.metadata.srv.CachedMetadataLoader;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * View for edit SAML trusted federation
 * 
 * @author P.Piernik
 *
 */
class EditTrustedFederationSubView extends VerticalLayout implements UnitySubView
{
	private MessageSource msg;
	private NotificationPresenter notificationPresenter;
	private Binder<SAMLServiceTrustedFederationConfiguration> binder;
	private boolean editMode = false;
	private Set<String> validators;
	private Set<String> certificates;
	private Set<String> usedNames;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private TextField url;
	private ComboBox<String> httpsTruststore;

	EditTrustedFederationSubView(MessageSource msg, URIAccessService uriAccessService,
			FileStorageService fileStorageService, SAMLServiceTrustedFederationConfiguration toEdit,
			SubViewSwitcher subViewSwitcher, Set<String> usedNames, Set<String> validators,
			Set<String> certificates, Consumer<SAMLServiceTrustedFederationConfiguration> onConfirm,
			Runnable onCancel, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.validators = validators;
		this.certificates = certificates;
		this.usedNames = usedNames;
		this.notificationPresenter = notificationPresenter;

		editMode = toEdit != null;
		binder = new Binder<>(SAMLServiceTrustedFederationConfiguration.class);
		FormLayout header = buildHeaderSection();
		binder.setBean(editMode ? toEdit.clone() : new SAMLServiceTrustedFederationConfiguration());
		AccordionPanel fetchMeta = buildFederationFetchSection();
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.add(header);
		mainView.add(fetchMeta);
		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditTrustedFederationSubView.invalidConfiguration"), e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		mainView.add(buttonsLayout);

		add(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		TextField name = new TextField();
		binder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg::getMessage)).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg
								.getMessage("EditTrustedFederationSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind("name");
		header.addFormItem(name, msg.getMessage("EditTrustedFederationSubView.name"));
		name.focus();

		url = new TextField();
		url.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(url).asRequired(msg.getMessage("fieldRequired")).bind("url");
		header.addFormItem(url, msg.getMessage("EditTrustedFederationSubView.url"));

		httpsTruststore = new ComboBox<>();
		httpsTruststore.setItems(validators);
		binder.forField(httpsTruststore).bind("httpsTruststore");
		header.addFormItem(httpsTruststore, msg.getMessage("EditTrustedFederationSubView.httpsTruststore"));

		Checkbox ignoreSignatureVerification = new Checkbox(
				msg.getMessage("EditTrustedFederationSubView.ignoreSignatureVerification"));
		binder.forField(ignoreSignatureVerification).bind("ignoreSignatureVerification");
		header.addFormItem(ignoreSignatureVerification, "");

		Select<String> signatureVerificationCertificate = new Select<>();
		signatureVerificationCertificate.setItems(certificates);
		signatureVerificationCertificate.setEmptySelectionAllowed(true);
		header.addFormItem(signatureVerificationCertificate, msg.getMessage("EditTrustedFederationSubView.signatureVerificationCertificate"));

		TextField refreshInterval = new TextField();
		binder.forField(refreshInterval).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshInterval");
		header.addFormItem(refreshInterval, msg.getMessage("EditTrustedFederationSubView.refreshInterval"));

		return header;
	}

	private AccordionPanel buildFederationFetchSection()
	{
		VerticalLayout federationListLayout = new VerticalLayout();
		federationListLayout.setMargin(false);
		ProgressBar spinner = new ProgressBar();
		spinner.setIndeterminate(true);
		spinner.setVisible(false);
		spinner.setWidth("1em");
		federationListLayout.add(spinner);
		GridWithActionColumn<SAMLEntity> samlEntities = new GridWithActionColumn<>(msg::getMessage,
				Collections.emptyList());
		samlEntities.addColumn(v -> v.name)
				.setHeader(msg.getMessage("EditTrustedFederationSubView.name"))
				.setAutoWidth(true);
		samlEntities.addColumn(v -> v.id)
				.setHeader(msg.getMessage("EditTrustedFederationSubView.entityIdentifier"))
				.setAutoWidth(true);
		samlEntities.removeActionColumn();

		SearchField search = GridSearchFieldFactory.generateSearchField(samlEntities, msg::getMessage);
		Toolbar<SAMLEntity> toolbar = new Toolbar<>();
		toolbar.setWidthFull();
		toolbar.setJustifyContentMode(JustifyContentMode.END);
		toolbar.addSearch(search);
		ComponentWithToolbar samlEntitiesListWithToolbar = new ComponentWithToolbar(samlEntities, toolbar);
		samlEntitiesListWithToolbar.setSpacing(false);
		samlEntitiesListWithToolbar.setSizeFull();
		samlEntitiesListWithToolbar.setVisible(false);
		Button fetch = new Button(msg.getMessage("EditTrustedFederationSubView.fetch"));
		UI ui = UI.getCurrent();

		CachedMetadataLoader metaDownloader = new CachedMetadataLoader(uriAccessService,
			fileStorageService);
		SimpleIDPMetaConverter convert = new SimpleIDPMetaConverter(msg);

		try
		{
			Optional<EntitiesDescriptorDocument> cached = metaDownloader.getCached(url.getValue());
			if(cached.isPresent())
			{
				List<SAMLEntity> entries = convert
					.getEntries(cached.get().getEntitiesDescriptor());
				samlEntities.setItems(entries);
				samlEntitiesListWithToolbar.setVisible(true);
			}
		}
		catch (Exception e)
		{
			ui.access(() -> notificationPresenter.showError("", e.getMessage()));
		}

		fetch.addClickListener(e -> {
			spinner.setVisible(true);
			CompletableFuture.runAsync(() ->
			{
				try
				{
					EntitiesDescriptorDocument entDoc = metaDownloader.getCached(url.getValue())
							.orElse(null);
					if (entDoc == null)
					{
						entDoc = metaDownloader.getFresh(url.getValue(),
								httpsTruststore.getValue());
					}
					List<SAMLEntity> entries = convert
							.getEntries(entDoc.getEntitiesDescriptor());
					samlEntities.setItems(entries);
					samlEntitiesListWithToolbar.setVisible(true);

				} catch (Exception e1)
				{
					ui.access(() ->
					{
						notificationPresenter.showError(e1.getMessage(), e1.getCause().getMessage());
						spinner.setVisible(false);
					});
					samlEntities.setItems(Collections.emptyList());
					samlEntitiesListWithToolbar.setVisible(false);
				}

				ui.access(() ->
				{
					ui.setPollInterval(-1);
					spinner.setVisible(false);
				});
			});

		});

		url.addValueChangeListener(e -> fetch.setEnabled(e.getValue() != null && !e.getValue().isEmpty()));

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setAlignItems(Alignment.CENTER);
		wrapper.setMargin(false);
		wrapper.add(fetch, spinner);

		federationListLayout.add(wrapper);
		federationListLayout.add(samlEntitiesListWithToolbar);
		AccordionPanel accordionPanel = new AccordionPanel(
				msg.getMessage("EditTrustedFederationSubView.serviceProviders"),
				federationListLayout);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditTrustedFederationSubView.trustedFederation"),
					binder.getBean().getName());
		else
			return Collections.singletonList(msg.getMessage("EditTrustedFederationSubView.newTrustedFederation"));

	}

	private SAMLServiceTrustedFederationConfiguration getTrustedFederation() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

}
