package io.github.rdrmic.mop.testassignment.mock;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class MockDataProvider {
	
	private static final String BUNDLE_NAME = "io.github.rdrmic.mop.testassignment.mock.mock_data";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private MockDataProvider() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
