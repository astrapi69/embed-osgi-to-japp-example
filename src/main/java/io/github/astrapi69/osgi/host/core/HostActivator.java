package io.github.astrapi69.osgi.host.core;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import io.github.astrapi69.awt.screen.position.ComponentPositionStore;
import io.github.astrapi69.osgi.host.service.lookup.Lookup;

public class HostActivator implements BundleActivator
{
	private Map m_lookupMap = null;
	private BundleContext m_context = null;
	private ServiceRegistration m_registration = null;

	public HostActivator(Map lookupMap)
	{
		// Save a reference to the service's backing store.
		m_lookupMap = lookupMap;
	}

	public void start(BundleContext context)
	{
		// Save a reference to the bundle context.
		m_context = context;
		// Create a property lookup service implementation.
		Lookup lookup = new Lookup()
		{
			public Object lookup(String name)
			{
				return m_lookupMap.get(name);
			}
		};
		// Register the property lookup service and save
		// the service registration.
		m_registration = m_context.registerService(Lookup.class.getName(), lookup, null);
		// create a component for instance a JFrame...
		JFrame frame = new JFrame("JFrame from osgi");
		JPanel panel = new JPanel();
		JLabel label = new JLabel("bla");
		panel.add(label);
		panel.setPreferredSize(new Dimension(400, 300));
		frame.getContentPane().setSize(800, 400);
		frame.getContentPane().add(panel);
		// create a store for the component position
		final ComponentPositionStore componentPositionStore = new ComponentPositionStore(frame,
			HostActivator.class, 600, 500);
		// restore the component position...
		componentPositionStore.restorePosition();
		// add a shutdown hook...
		Runtime.getRuntime().addShutdownHook(new Thread(componentPositionStore::storePosition));
		// show the frame...
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.pack();
	}

	public void stop(BundleContext context)
	{
		// Unregister the property lookup service.
		m_registration.unregister();
		m_context = null;
	}
}