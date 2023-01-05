/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.console.SimpleIDPMetaConverter.SAMLEntity;
import pl.edu.icm.unity.saml.metadata.srv.CachedMetadataLoader;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * View for edit SAML trusted federation
 * 
 * @author P.Piernik
 *
 */
class EditTrustedFederationSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
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
			Runnable onCancel)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.validators = validators;
		this.certificates = certificates;
		this.usedNames = usedNames;

		editMode = toEdit != null;
		binder = new Binder<>(SAMLServiceTrustedFederationConfiguration.class);
		FormLayout header = buildHeaderSection();
		binder.setBean(editMode ? toEdit.clone() : new SAMLServiceTrustedFederationConfiguration());
		CollapsibleLayout fetchMeta = buildFederationFetchSection();
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(fetchMeta);

		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("EditTrustedFederationSubView.invalidConfiguration"), e);
			}
		};
		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField(msg.getMessage("EditTrustedFederationSubView.name"));
		binder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg)).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg
								.getMessage("EditTrustedFederationSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind("name");
		header.addComponent(name);
		name.focus();

		url = new TextField(msg.getMessage("EditTrustedFederationSubView.url"));
		url.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		binder.forField(url).asRequired(msg.getMessage("fieldRequired")).bind("url");
		header.addComponent(url);

		httpsTruststore = new ComboBox<>(msg.getMessage("EditTrustedFederationSubView.httpsTruststore"));
		httpsTruststore.setItems(validators);
		binder.forField(httpsTruststore).bind("httpsTruststore");
		header.addComponent(httpsTruststore);

		CheckBox ignoreSignatureVerification = new CheckBox(
				msg.getMessage("EditTrustedFederationSubView.ignoreSignatureVerification"));
		binder.forField(ignoreSignatureVerification).bind("ignoreSignatureVerification");
		header.addComponent(ignoreSignatureVerification);

		ComboBox<String> signatureVerificationCertificate = new ComboBox<>(
				msg.getMessage("EditTrustedFederationSubView.signatureVerificationCertificate"));
		signatureVerificationCertificate.setItems(certificates);
		header.addComponent(signatureVerificationCertificate);

		TextField refreshInterval = new TextField();
		refreshInterval.setCaption(msg.getMessage("EditTrustedFederationSubView.refreshInterval"));
		binder.forField(refreshInterval).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshInterval");
		header.addComponent(refreshInterval);

		return header;
	}

	private CollapsibleLayout buildFederationFetchSection()
	{
		VerticalLayout federationListLayout = new VerticalLayout();
		federationListLayout.setMargin(false);
		ProgressBar spinner = new ProgressBar();
		spinner.setIndeterminate(true);
		spinner.setVisible(false);
		federationListLayout.addComponent(spinner);
		GridWithActionColumn<SAMLEntity> samlEntities = new GridWithActionColumn<>(msg,
				Collections.emptyList());
		samlEntities.setActionColumnHidden(true);
		samlEntities.setHeightByRows(false);
		samlEntities.setHeightByRows(14);
		samlEntities.addColumn(v -> v.name, msg.getMessage("EditTrustedFederationSubView.name"), 40);
		samlEntities.addColumn(v -> v.id, msg.getMessage("EditTrustedFederationSubView.entityIdentifier"), 40);

		SearchField search = FilterableGridHelper.generateSearchField(samlEntities, msg);
		Toolbar<SAMLEntity> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addSearch(search, Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar samlEntitiesListWithToolbar = new ComponentWithToolbar(samlEntities, toolbar,
				Alignment.BOTTOM_LEFT);
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
			ui.access(() -> {
				NotificationPopup.showError(msg, "", e);
			});
		}

		fetch.addClickListener(e -> {
			ui.setPollInterval(500);
			spinner.setVisible(true);
			CompletableFuture.runAsync(() -> {

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
					ui.access(() -> {
						NotificationPopup.showError(msg, "", e1);
						ui.setPollInterval(-1);
						spinner.setVisible(false);
					});
					samlEntities.setItems(Collections.emptyList());
					samlEntitiesListWithToolbar.setVisible(false);
				}

				ui.access(() -> {
					ui.setPollInterval(-1);
					spinner.setVisible(false);
				});
			});

		});

		url.addValueChangeListener(e -> {
			if (e.getValue() == null || e.getValue().isEmpty())
			{
				fetch.setEnabled(false);
			} else
			{
				fetch.setEnabled(true);
			}
		});

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.addComponents(fetch, spinner);

		federationListLayout.addComponent(wrapper);
		federationListLayout.addComponent(samlEntitiesListWithToolbar);
		return new CollapsibleLayout(msg.getMessage("EditTrustedFederationSubView.serviceProviders"),
				federationListLayout);
	}

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditTrustedFederationSubView.trustedFederation"),
					binder.getBean().getName());
		else
			return Arrays.asList(msg.getMessage("EditTrustedFederationSubView.newTrustedFederation"));

	}

	private SAMLServiceTrustedFederationConfiguration getTrustedFederation() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

}
