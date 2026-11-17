package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.image.LinkableImage;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.base.verifiable.VerifiableMobileNumber;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

abstract class AttributeWidgetTestBase extends UserAccountTestBase
{
	final String name = "ui-widget-" + UUID.randomUUID();
	private TestUser user;
	private Long policyId;
	private UUID imageId;
	@TempDir
	Path temporaryDirectory;

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;
	@Autowired
	@Qualifier("insecure")
	private PolicyDocumentManagement policies;
	@Autowired
	private AttributeHandlerRegistry handlers;

	@AfterEach
	void removeSchema() throws Exception
	{
		try
		{
			if (types.getAttributeTypesAsMap().containsKey(name))
			{
				if (user != null && !attributes.getAttributes(user.entity(), "/", name).isEmpty())
					attributes.removeAttribute(user.entity(), "/", name);
				types.removeAttributeType(name, false);
			}
		} finally
		{
			if (policyId != null)
				policies.removePolicyDocument(policyId);
		}
	}

	final void prepare(AttributeWidgetCase scenario) throws Exception
	{
		assertThat(handlers.getSupportedSyntaxes()).containsExactlyInAnyOrderElementsOf(
				AttributeWidgetCase.cases().map(AttributeWidgetCase::syntax).distinct().toList());
		user = createUser("Attribute widget owner");
		AttributeType type = new AttributeType(name, scenario.syntax());
		type.setMinElements(1);
		type.setMaxElements(1);
		AttributeValueSyntax<?> syntax = handlers.getaTypeSupport().getSyntax(type);
		if (syntax instanceof StringAttributeSyntax string)
		{
			string.setMinLength(3);
			string.setMaxLength(80);
			string.setEditWithTextArea(scenario.widget() == AttributeWidgetCase.Widget.MULTILINE);
		}
		else if (syntax instanceof IntegerAttributeSyntax integer)
		{
			integer.setMin(10);
			integer.setMax(100);
		}
		else if (syntax instanceof FloatingPointAttributeSyntax floatingPoint)
		{
			floatingPoint.setMin(1);
			floatingPoint.setMax(20);
		}
		else if (syntax instanceof EnumAttributeSyntax enumeration)
			enumeration.setAllowed("blue", "green");
		type.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		types.addAttributeType(type);
		if (scenario.widget() == AttributeWidgetCase.Widget.POLICY)
			policyId = policies.addPolicyDocument(new PolicyDocumentCreateRequest(name, new I18nString(name), false,
					PolicyDocumentContentType.EMBEDDED, new I18nString("<p>Disposable test policy.</p>")));
		new AccountAdministrationPage(driver).open(baseUrl);
		new DirectoryWidgetPage(driver).selectEntity(user.entity());
	}

	final void assertRoundTrip(AttributeWidgetCase scenario) throws Exception
	{
		prepare(scenario);
		DirectoryWidgetPage directory = new DirectoryWidgetPage(driver);
		AttributeWidgetPage widget = new AttributeWidgetPage(driver);
		directory.addAttribute(name);
		fill(widget, scenario, scenario.initial(), false);
		directory.saveAttribute();
		assertStoredAndRendered(scenario, scenario.initial());

		reload(directory);
		directory.editAttribute(name);
		widget.assertValue(scenario, input(scenario, scenario.initial()));
		fill(widget, scenario, scenario.updated(), true);
		directory.saveAttribute();
		assertStoredAndRendered(scenario, scenario.updated());
		reload(directory);
		directory.editAttribute(name);
		widget.assertValue(scenario, input(scenario, scenario.updated()));
		screenshot("Reloaded attribute value editor");
		new VaadinPage(driver).click(testId("AttributeEditDialog", "DialogWithActionFooter.cancel"));
		assertStoredAndRendered(scenario, scenario.updated());
	}

	final void reload(DirectoryWidgetPage directory)
	{
		driver.navigate().refresh();
		new DirectoryPage(driver).selectGroup("/");
		directory.selectEntity(user.entity());
	}

	final void fill(AttributeWidgetPage widget, AttributeWidgetCase scenario, String value, boolean replacing)
			throws Exception
	{
		if (scenario.uploadsImage())
		{
			Path image = writeImage(value);
			widget.upload(image, replacing);
			waitFor(() -> Boolean.parseBoolean(widget.image().getDomProperty("complete"))
					&& Integer.parseInt(widget.image().getDomProperty("naturalWidth")) == imageWidth(value));
		}
		else
			widget.fill(scenario, input(scenario, value));
	}

	private Path writeImage(String value) throws Exception
	{
		int width = imageWidth(value);
		BufferedImage image = new BufferedImage(width, width / 2, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++)
			for (int y = 0; y < width / 2; y++)
				image.setRGB(x, y, value.equals("initial") ? 0xAA2233 : 0x3355CC);
		Path path = temporaryDirectory.resolve(value + ".png");
		ImageIO.write(image, "png", path.toFile());
		return path;
	}

	private static int imageWidth(String value)
	{
		return value.equals("initial") ? 32 : 24;
	}

	final String input(AttributeWidgetCase scenario, String value) throws Exception
	{
		return scenario.widget() == AttributeWidgetCase.Widget.POLICY
				? new PolicyAgreementState(policyId, 1, PolicyAgreementAcceptanceStatus.valueOf(value),
						Date.from(Instant.parse("2030-05-08T12:00:00Z"))).toJson() : value;
	}

	final String storedValue() throws Exception
	{
		List<AttributeExt> stored = List.copyOf(attributes.getAttributes(user.entity(), "/", name));
		assertThat(stored).singleElement().satisfies(attribute ->
		{
			assertThat(attribute.getName()).isEqualTo(name);
			assertThat(attribute.getGroupPath()).isEqualTo("/");
			assertThat(attribute.getValues()).hasSize(1);
		});
		return stored.getFirst().getValues().getFirst();
	}

	final void assertStoredAndRendered(AttributeWidgetCase scenario, String expected) throws Exception
	{
		String stored = storedValue();
		String rendered = new GridPage(driver).text("AttributesGrid.attributes", "attribute-" + name);
		assertThat(rendered).contains(name);
		switch (scenario.widget())
		{
			case EMAIL -> assertThat(VerifiableEmail.fromJsonString(stored).getValue()).isEqualTo(expected);
			case MOBILE -> assertThat(VerifiableMobileNumber.fromJsonString(stored).getValue()).isEqualTo(expected);
			case DATETIME -> assertThat(LocalDateTime.parse(stored)).isEqualTo(LocalDateTime.parse(expected));
			case ZONED_DATETIME -> assertThat(ZonedDateTime.parse(stored))
					.usingComparator(ZonedDateTime::compareTo).isEqualTo(ZonedDateTime.parse(expected));
			case POLICY -> assertThat(PolicyAgreementState.fromJson(stored)).isEqualTo(
					PolicyAgreementState.fromJson(input(scenario, expected)));
			case IMAGE, LINKED_IMAGE -> assertImage(scenario, stored, expected);
			case IMAGE_URL ->
			{
				LinkableImage image = LinkableImage.valueOf(stored);
				assertThat(image.getUnityImage()).isNull();
				assertThat(image.getUrl().toExternalForm()).isEqualTo(expected);
				assertImageId(image);
			}
			default ->
			{
				if (scenario.syntax().equals("json"))
					assertThat(JsonUtil.parse(stored)).isEqualTo(JsonUtil.parse(expected));
				else
					assertThat(stored).isEqualTo(expected);
			}
		}
		if (scenario.widget() == AttributeWidgetCase.Widget.POLICY)
			assertThat(rendered).contains(name, expected.equals("ACCEPTED") ? "ACCEPTED" : "DECLINED");
		if (scenario.widget() == AttributeWidgetCase.Widget.DATETIME
				|| scenario.widget() == AttributeWidgetCase.Widget.ZONED_DATETIME)
			assertThat(rendered).contains(stored);
		if (!scenario.uploadsImage()
				&& scenario.widget() != AttributeWidgetCase.Widget.POLICY
				&& scenario.widget() != AttributeWidgetCase.Widget.DATETIME
				&& scenario.widget() != AttributeWidgetCase.Widget.ZONED_DATETIME)
			assertThat(rendered.replace('\n', ' ')).contains(expected.replace('\n', ' '));
	}

	private void assertImage(AttributeWidgetCase scenario, String stored, String expected) throws Exception
	{
		UnityImage image;
		if (scenario.widget() == AttributeWidgetCase.Widget.LINKED_IMAGE)
		{
			LinkableImage linkable = LinkableImage.valueOf(stored);
			assertImageId(linkable);
			assertThat(linkable.getUrl()).isNull();
			image = linkable.getUnityImage();
		}
		else
			image = new UnityImage(stored);
		assertThat(image.getWidth()).isEqualTo(imageWidth(expected));
		assertThat(image.getHeight()).isEqualTo(imageWidth(expected) / 2);
		assertThat(image.getBufferedImage().getRGB(0, 0) & 0xFFFFFF)
				.isEqualTo(expected.equals("initial") ? 0xAA2233 : 0x3355CC);
		WebElement preview = new VaadinPage(driver).visible(By.cssSelector("[data-testid='AttributesGrid.attributes'] img"));
		waitFor(() -> Boolean.parseBoolean(preview.getDomProperty("complete"))
				&& Integer.parseInt(preview.getDomProperty("naturalWidth")) > 0);
	}

	private void assertImageId(LinkableImage image)
	{
		assertThat(image.getExternalId()).isNotNull();
		if (imageId == null)
			imageId = image.getExternalId();
		assertThat(image.getExternalId()).isEqualTo(imageId);
	}

}
