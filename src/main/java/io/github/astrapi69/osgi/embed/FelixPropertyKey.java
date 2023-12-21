package io.github.astrapi69.osgi.embed;

import lombok.Getter;

public enum FelixPropertyKey
{

	SHUTDOWN_HOOK(FelixPropertyKey.SHUTDOWN_HOOK_PROPERTY_KEY), SYSTEM_PROPERTIES(
		FelixPropertyKey.SYSTEM_PROPERTIES_PROPERTY_KEY), CONFIG_PROPERTIES(
			FelixPropertyKey.CONFIG_PROPERTIES_PROPERTY_KEY);

	/**
	 * The property name used to specify an URL to the configuration property file to be used for
	 * the created the framework instance.
	 **/
	public static final String CONFIG_PROPERTIES_PROPERTY_KEY = "felix.config.properties";

	/**
	 * The property name used to specify whether the launcher should install a shutdown hook.
	 **/
	public static final String SHUTDOWN_HOOK_PROPERTY_KEY = "felix.shutdown.hook";

	/**
	 * The property name used to specify an URL to the system property file.
	 **/
	public static final String SYSTEM_PROPERTIES_PROPERTY_KEY = "felix.system.properties";
	@Getter
	private final String key;

	FelixPropertyKey(String key)
	{
		this.key = key;
	}
}
