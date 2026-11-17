package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;

@Tag("home")
class ProfileImageTest extends HomeWorkflowTestBase
{
	private final String attributeName = "ui-profile-image-" + UUID.randomUUID();
	private TestUser owner;
	@TempDir
	Path temporaryDirectory;

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@AfterEach
	void removeImageType() throws Exception
	{
		if (types.getAttributeTypesAsMap().containsKey(attributeName))
		{
			if (owner != null && !attributes.getAttributes(owner.entity(), "/", attributeName).isEmpty())
				attributes.removeAttribute(owner.entity(), "/", attributeName);
			types.removeAttributeType(attributeName, false);
		}
	}

	@Test
	void shouldUploadReloadReplaceAndResetProfileImageWithoutChangingAnotherAccount() throws Exception
	{
		owner = createUser("Image owner");
		TestUser control = createUser("Image control");
		AttributeType type = new AttributeType(attributeName, "image");
		type.setSelfModificable(true);
		type.setMaxElements(1);
		type.setValueSyntaxConfiguration(new ImageAttributeSyntax().getSerializedConfiguration());
		types.addAttributeType(type);
		deployHome("""
				unity.userhome.attributes.1.attribute=%s
				unity.userhome.attributes.1.group=/
				unity.userhome.attributes.1.editable=true
				""".formatted(attributeName));
		VaadinPage page = openProfile(owner);
		Path first = image("first", 32, 0xAA2233);
		Path replacement = image("replacement", 24, 0x3355CC);

		page.click(By.cssSelector("[data-testid='ProfileView.attribute'][data-attribute-name='" + attributeName
				+ "'] vaadin-icon[icon='vaadin:plus-circle-o']"));
		upload(page, first, 32);
		page.click(testId("ProfileView.save"));
		waitForElementNotPresent(testId("ProfileView.save"));
		assertStored(32, 0xAA2233);
		driver.navigate().refresh();
		assertPreview(page, 32);

		page.click(testId("ProfileView.attribute", "UnityImageValueComponent.clear"));
		upload(page, replacement, 24);
		page.click(testId("ProfileView.reset"));
		waitForElementNotPresent(testId("ProfileView.reset"));
		assertPreview(page, 32);
		assertStored(32, 0xAA2233);

		page.click(testId("ProfileView.attribute", "UnityImageValueComponent.clear"));
		upload(page, replacement, 24);
		page.click(testId("ProfileView.save"));
		waitForElementNotPresent(testId("ProfileView.save"));
		driver.navigate().refresh();
		assertPreview(page, 24);
		assertStored(24, 0x3355CC);
		assertThat(attributes.getAttributes(control.entity(), "/", attributeName)).isEmpty();
		screenshot("Saved profile image");
	}

	private Path image(String name, int width, int color) throws Exception
	{
		BufferedImage image = new BufferedImage(width, width / 2, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++)
			for (int y = 0; y < width / 2; y++)
				image.setRGB(x, y, color);
		Path file = temporaryDirectory.resolve(name + ".png");
		ImageIO.write(image, "png", file.toFile());
		return file;
	}

	private void upload(VaadinPage page, Path file, int width)
	{
		page.visible(testId("ProfileView.attribute", "UnityImageValueComponent.upload")).getShadowRoot()
				.findElement(By.cssSelector("input[type='file']")).sendKeys(file.toAbsolutePath().toString());
		assertPreview(page, width);
	}

	private void assertPreview(VaadinPage page, int width)
	{
		waitFor(() ->
		{
			WebElement preview = page.visible(testId("ProfileView.attribute", "UnityImageValueComponent.preview"));
			return Boolean.parseBoolean(preview.getDomProperty("complete"))
					&& Integer.parseInt(preview.getDomProperty("naturalWidth")) == width
					&& Integer.parseInt(preview.getDomProperty("naturalHeight")) == width / 2;
		});
	}

	private void assertStored(int width, int color) throws Exception
	{
		assertThat(attributes.getAttributes(owner.entity(), "/", attributeName)).singleElement();
		String value = attributes.getAttributes(owner.entity(), "/", attributeName).iterator().next()
				.getValues().getFirst().toString();
		UnityImage image = new UnityImage(value);
		assertThat(image.getWidth()).isEqualTo(width);
		assertThat(image.getHeight()).isEqualTo(width / 2);
		assertThat(image.getBufferedImage().getRGB(0, 0) & 0xFFFFFF).isEqualTo(color);
	}
}
