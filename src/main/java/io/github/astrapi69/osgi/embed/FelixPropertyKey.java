package io.github.astrapi69.osgi.embed;

import lombok.Getter;

/**
 * The enum class {@link FelixPropertyKey} provides constants for the property keys from the
 * configuration file
 */
public enum FelixPropertyKey
{

	/**
	 * The enum value for the property name used to specify whether the launcher should install a
	 * shutdown hook
	 */
	SHUTDOWN_HOOK(FelixPropertyKey.SHUTDOWN_HOOK_PROPERTY_KEY),

	/**
	 * The enum value for the property name used to specify the URL to the system property file
	 */
	SYSTEM_PROPERTIES(FelixPropertyKey.SYSTEM_PROPERTIES_PROPERTY_KEY),

	/**
	 * The enum value for the property name used to specify the URL to the configuration property
	 * file to be used for the created the framework instance
	 */
	CONFIG_PROPERTIES(FelixPropertyKey.CONFIG_PROPERTIES_PROPERTY_KEY);

	/**
	 * The property name used to specify the URL to the configuration property file to be used for
	 * the created the framework instance
	 */
	public static final String CONFIG_PROPERTIES_PROPERTY_KEY = "felix.config.properties";

	/**
	 * The property name used to specify whether the launcher should install a shutdown hook
	 */
	public static final String SHUTDOWN_HOOK_PROPERTY_KEY = "felix.shutdown.hook";

	/**
	 * The property name used to specify an URL to the system property file
	 */
	public static final String SYSTEM_PROPERTIES_PROPERTY_KEY = "felix.system.properties";

	/**
	 * The property name of the key
	 */
	@Getter
	private final String key;

	/**
	 * Creates a new enum {@link FelixPropertyKey} object
	 * 
	 * @param key
	 *            The property name of the key
	 */
	private FelixPropertyKey(String key)
	{
		this.key = key;
	}
}
