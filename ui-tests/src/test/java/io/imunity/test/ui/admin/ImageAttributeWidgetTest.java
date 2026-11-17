package io.imunity.test.ui.admin;

import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("admin-directory")
class ImageAttributeWidgetTest extends AttributeWidgetTestBase
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("imageCases")
	void shouldCreateEditReloadAndRenderImageValue(AttributeWidgetCase scenario) throws Exception
	{
		assertRoundTrip(scenario);
	}

	private static Stream<AttributeWidgetCase> imageCases()
	{
		return AttributeWidgetCase.cases().filter(AttributeWidgetCase::usesImage);
	}
}
