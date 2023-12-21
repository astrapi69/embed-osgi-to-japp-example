package io.github.astrapi69.osgi.embed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;

import io.github.astrapi69.file.search.PathFinder;

public class FelixEmbeddedApplication
{
	/**
	 * Switch for specifying bundle directory.
	 **/
	public static final String BUNDLE_DIR_SWITCH = "-b";

	/**
	 * The default name used for the system properties file.
	 **/
	public static final String SYSTEM_PROPERTIES_FILE_VALUE = "system.properties";
	/**
	 * The default name used for the configuration properties file.
	 **/
	public static final String CONFIG_PROPERTIES_FILE_VALUE = "conf/config.properties";
	/**
	 * Name of the configuration directory.
	 */
	public static final String CONFIG_DIRECTORY = "conf";

	private static Framework framework;

	public static void main(String[] args) throws Exception
	{
		// (1) Check for command line arguments and verify usage.
		String bundleDir = null;
		String cacheDir = null;
		boolean expectBundleDir = false;
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals(BUNDLE_DIR_SWITCH))
			{
				expectBundleDir = true;
			}
			else if (expectBundleDir)
			{
				bundleDir = args[i];
				expectBundleDir = false;
			}
			else
			{
				cacheDir = args[i];
			}
		}

		if ((args.length > 3) || (expectBundleDir && bundleDir == null))
		{
			System.out.println("Usage: [-b <bundle-deploy-dir>] [<bundle-cache-dir>]");
			System.exit(0);
		}

		// (2) Load system properties.
		FelixEmbeddedApplication.loadSystemProperties();

		// (3) Read configuration properties.
		Map<String, String> configProps = FelixEmbeddedApplication.loadConfigProperties();
		if (configProps == null)
		{
			System.err.println("No " + CONFIG_PROPERTIES_FILE_VALUE + " found.");
			configProps = new HashMap<String, String>();
		}

		// (4) Copy framework properties from the system properties.
		FelixEmbeddedApplication.copySystemProperties(configProps);

		// (5) Use the specified auto-deploy directory over default.
		if (bundleDir != null)
		{
			configProps.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
		}

		// (6) Use the specified bundle cache directory over default.
		if (cacheDir != null)
		{
			configProps.put(Constants.FRAMEWORK_STORAGE, cacheDir);
		}

		// (7) Add a shutdown hook to clean stop the framework.
		String enableHook = configProps.get(FelixPropertyKey.SHUTDOWN_HOOK.getKey());
		if ((enableHook == null) || !enableHook.equalsIgnoreCase("false"))
		{
			Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook")
			{
				public void run()
				{
					try
					{
						if (framework != null)
						{
							framework.stop();
							framework.waitForStop(0);
						}
					}
					catch (Exception ex)
					{
						System.err.println("Error stopping framework: " + ex);
					}
				}
			});
		}

		try
		{
			// (8) Create an instance and initialize the framework.
			FrameworkFactory frameworkFactory = getFrameworkFactory();

			framework = newFramework(frameworkFactory, configProps);
			framework.init();
			// (9) Use the system bundle context to process the auto-deploy
			// and auto-install/auto-start properties.
			AutoProcessor.process(configProps, framework.getBundleContext());
			// (10) Start the framework.
			FrameworkEvent event;
			do
			{
				// Start the framework.
				framework.start();
				// Wait for framework to stop to exit the VM.
				event = framework.waitForStop(0);
			}
			// If the framework was updated, then restart it.
			while (event.getType() == FrameworkEvent.STOPPED_UPDATE);
			// Otherwise, exit.
			System.exit(0);
		}
		catch (Exception ex)
		{
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
			System.exit(0);
		}
	}

	private static Framework newFramework(FrameworkFactory frameworkFactory,
		Map<String, String> configProps)
	{
		return frameworkFactory.newFramework(configProps);
	}

	/**
	 * Simple method to parse META-INF/services file for framework factory. Currently, it assumes
	 * the first non-commented line is the class name of the framework factory implementation.
	 * 
	 * @return The created <tt>FrameworkFactory</tt> instance.
	 * @throws Exception
	 *             if any errors occur.
	 **/
	private static FrameworkFactory getFrameworkFactory() throws Exception
	{
		URL url = FelixEmbeddedApplication.class.getClassLoader()
			.getResource("META-INF/services/org.osgi.framework.launch.FrameworkFactory");
		if (url != null)
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			try
			{
				for (String s = br.readLine(); s != null; s = br.readLine())
				{
					s = s.trim();
					// Try to load first non-empty, non-commented line.
					if ((s.length() > 0) && (s.charAt(0) != '#'))
					{
						return (FrameworkFactory)Class.forName(s).newInstance();
					}
				}
			}
			finally
			{
				if (br != null)
					br.close();
			}
		}

		throw new Exception("Could not find framework factory.");
	}

	/**
	 * <p>
	 * Loads the properties in the system property file associated with the framework installation
	 * into <tt>System.setProperty()</tt>. These properties are not directly used by the framework
	 * in anyway. By default, the system property file is located in the <tt>conf/</tt> directory of
	 * the Felix installation directory and is called "<tt>system.properties</tt>". The installation
	 * directory of Felix is assumed to be the parent directory of the <tt>felix.jar</tt> file as
	 * found on the system class path property. The precise file from which to load system
	 * properties can be set by initializing the "<tt>felix.system.properties</tt>" system property
	 * to an arbitrary URL.
	 * </p>
	 **/
	public static void loadSystemProperties()
	{
		// The system properties file is either specified by a system
		// property or it is in the same directory as the Felix JAR file.
		// Try to load it from one of these places.

		// See if the property URL was specified as a property.
		URL propURL = null;
		String custom = System.getProperty(FelixPropertyKey.SYSTEM_PROPERTIES.getKey());
		if (custom != null)
		{
			try
			{
				propURL = new URL(custom);
			}
			catch (MalformedURLException ex)
			{
				System.err.print("Main: " + ex);
				return;
			}
		}
		else
		{
			// Determine where the configuration directory is by figuring
			// out where felix.jar is located on the system class path.
			File confDir = null;
			String classpath = System.getProperty("java.class.path");
			int index = classpath.toLowerCase().indexOf("felix.jar");
			int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
			if (index >= start)
			{
				// Get the path of the felix.jar file.
				String jarLocation = classpath.substring(start, index);
				// Calculate the conf directory based on the parent
				// directory of the felix.jar directory.
				confDir = new File(new File(new File(jarLocation).getAbsolutePath()).getParent(),
					CONFIG_DIRECTORY);
			}
			else
			{
				// Can't figure it out so use the current directory as default.
				confDir = new File(System.getProperty("user.dir"), CONFIG_DIRECTORY);
			}

			try
			{
				propURL = new File(confDir, SYSTEM_PROPERTIES_FILE_VALUE).toURL();
			}
			catch (MalformedURLException ex)
			{
				System.err.print("Main: " + ex);
				return;
			}
		}

		// Read the properties file.
		Properties props = new Properties();
		InputStream is = null;
		try
		{
			is = propURL.openConnection().getInputStream();
			props.load(is);
			is.close();
		}
		catch (FileNotFoundException ex)
		{
			// Ignore file not found.
		}
		catch (Exception ex)
		{
			System.err.println("Main: Error loading system properties from " + propURL);
			System.err.println("Main: " + ex);
			try
			{
				if (is != null)
					is.close();
			}
			catch (IOException ex2)
			{
				// Nothing we can do.
			}
			return;
		}

		// Perform variable substitution on specified properties.
		for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
		{
			String name = (String)e.nextElement();
			System.setProperty(name, Util.substVars(props.getProperty(name), name, null, null));
		}
	}


	/**
	 * <p>
	 * Loads the configuration properties in the configuration property file associated with the
	 * framework installation; these properties are accessible to the framework and to bundles and
	 * are intended for configuration purposes. By default, the configuration property file is
	 * located in the <tt>conf/</tt> directory of the Felix installation directory and is called
	 * "<tt>config.properties</tt>". The installation directory of Felix is assumed to be the parent
	 * directory of the <tt>felix.jar</tt> file as found on the system class path property. The
	 * precise file from which to load configuration properties can be set by initializing the
	 * "<tt>felix.config.properties</tt>" system property to an arbitrary URL.
	 * </p>
	 * 
	 * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
	 **/
	public static Map<String, String> loadConfigProperties()
	{
		// The config properties file is either specified by a system
		// property or it is in the conf/ directory of the Felix
		// installation directory. Try to load it from one of these
		// places.

		// See if the property URL was specified as a property.
		URL propURL = null;
		String custom = System.getProperty(FelixPropertyKey.CONFIG_PROPERTIES.getKey());
		if (custom != null)
		{
			try
			{
				propURL = new URL(custom);
			}
			catch (MalformedURLException ex)
			{
				System.err.print("Main: " + ex);
				return null;
			}
		}
		else
		{
			// Determine where the configuration directory is by figuring
			// out where felix.jar is located on the system class path.
			File confDir = null;
			String classpath = System.getProperty("java.class.path");
			int index = classpath.toLowerCase().indexOf("felix.jar");
			int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
			if (index >= start)
			{
				// Get the path of the felix.jar file.
				String jarLocation = classpath.substring(start, index);
				// Calculate the conf directory based on the parent
				// directory of the felix.jar directory.
				confDir = new File(new File(new File(jarLocation).getAbsolutePath()).getParent(),
					CONFIG_DIRECTORY);
			}
			else
			{
				// Can't figure it out so use the current directory as default.
				confDir = new File(PathFinder.getSrcMainResourcesDir(), CONFIG_DIRECTORY);
			}

			try
			{
				propURL = new File(confDir, CONFIG_PROPERTIES_FILE_VALUE).toURL();
			}
			catch (MalformedURLException ex)
			{
				System.err.print("Main: " + ex);
				return null;
			}
		}

		// Read the properties file.
		Properties props = new Properties();
		InputStream is = null;
		try
		{
			// Try to load config.properties.
			is = propURL.openConnection().getInputStream();
			props.load(is);
			is.close();
		}
		catch (Exception ex)
		{
			// Try to close input stream if we have one.
			try
			{
				if (is != null)
					is.close();
			}
			catch (IOException ex2)
			{
				// Nothing we can do.
			}

			return null;
		}

		// Perform variable substitution for system properties and
		// convert to dictionary.
		Map<String, String> map = new HashMap<String, String>();
		for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
		{
			String name = (String)e.nextElement();
			map.put(name, Util.substVars(props.getProperty(name), name, null, props));
		}

		return map;
	}


	public static void copySystemProperties(Map configProps)
	{
		for (Enumeration e = System.getProperties().propertyNames(); e.hasMoreElements();)
		{
			String key = (String)e.nextElement();
			if (key.startsWith("felix.") || key.startsWith("org.osgi.framework."))
			{
				configProps.put(key, System.getProperty(key));
			}
		}
	}
}
