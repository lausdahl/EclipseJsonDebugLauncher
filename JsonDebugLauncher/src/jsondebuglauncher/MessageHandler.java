package jsondebuglauncher;

import java.io.BufferedReader;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.intocps.orchestration.coe.httpserver.Response;

public class MessageHandler implements RequestHandler
{

	@Override
	public Response doPost(Properties header, String url,
			BufferedReader dataReader)
	{
		return process(url);

	}

	@Override
	public Response doPut(Properties header, String url,
			BufferedReader dataReader)
	{
		return process(url);

	}

	@Override
	public Response doGet(Properties header, String url,
			BufferedReader dataReader)
	{
		return process(url);
	}

	public Response process(String url)
	{
		String configName = url.substring(url.indexOf('/')+1);

		ILaunchConfiguration[] launchConfigs;
		try
		{
			launchConfigs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();

			ILaunchConfiguration launchConfig = null;

			for (ILaunchConfiguration iLaunchConfiguration : launchConfigs)
			{
				if (iLaunchConfiguration.getName().equals(configName))
				{
					launchConfig = iLaunchConfiguration;
				}
			}

			if(launchConfig==null)
			{
				return new Response(Response.HTTP_NOTFOUND, Response.MIME_PLAINTEXT, "Launch config not found: '"+configName+"'");
			}
			DebugUITools.launch(launchConfig, ILaunchManager.DEBUG_MODE);
			
			return new Response(Response.HTTP_OK, Response.MIME_PLAINTEXT, "Launching "+configName+" now...");
		} catch (CoreException e)
		{
			Activator.log("Faild to find and launch config with name: "
					+ configName, e);
		}

		return new Response(Response.HTTP_INTERNALERROR, Response.MIME_PLAINTEXT, "internal error, unable to launch");
	}

}
