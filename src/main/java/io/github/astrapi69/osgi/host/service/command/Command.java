package io.github.astrapi69.osgi.host.service.command;

public interface Command
{
	String getName();

	String getDescription();

	boolean execute(String commandline);
}