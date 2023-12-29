package io.github.astrapi69.osgi.host.app;

import io.github.astrapi69.osgi.host.core.HostActivator;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostApplication
{
	private HostActivator m_activator = null;
	private Felix m_felix = null;
	private Framework framework;
	private Map m_lookupMap = new HashMap();

	public static void main(String[] args) throws Exception
	{
		HostApplication application = new HostApplication();
	}

	public HostApplication()
	{
		// Initialize the map for the property lookup service.
		m_lookupMap.put("name1", "value1");

		m_lookupMap.put("name2", "value2");
		m_lookupMap.put("name3", "value3");
		m_lookupMap.put("name4", "value4");

		// Create a configuration property map.
		Map configMap = new HashMap();
		// Export the host provided service interface package.
		configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
			"io.github.astrapi69.osgi.host.service.search; version=1.0.0\n" +
					"io.github.mnl.osgiGettingStarted.simpleBundle;  version=1.0.0");
		// Create host activator;
		m_activator = new HostActivator(m_lookupMap);
		List list = new ArrayList();
		list.add(m_activator);
		configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);

		try
		{
			// Now create an instance of the framework with
			// our configuration properties.
			m_felix = new Felix(configMap);
			BundleContext bundleContext = m_felix.getBundleContext();

			while (bundleContext == null) {
				bundleContext = m_felix.getBundleContext();
				Thread.sleep(100);
			}

			// Now start Felix instance.
			m_felix.start();
		}
		catch (Exception ex)
		{
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
		}
	}

	public void shutdownApplication() throws BundleException, InterruptedException
	{
		// Shut down the felix framework when stopping the
		// host application.
		m_felix.stop();
		m_felix.waitForStop(0);
	}
}