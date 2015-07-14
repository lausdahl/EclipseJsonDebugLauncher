package jsondebuglauncher;

import java.io.IOException;

import org.intocps.orchestration.coe.httpserver.NanoHTTPDJson;

public class JsonDebugLauncherServer
{
	final static int port = 8893;

	public void start()
	{
		try
		{
			new NanoHTTPDJson(port, new MessageHandler());
		} catch (IOException ioe)
		{
			Activator.log("Couldn't start server:\n" , ioe);
		}
	}

	public void stop()
	{

	}

}
