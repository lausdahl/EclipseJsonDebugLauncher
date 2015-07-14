package org.intocps.orchestration.coe.httpserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 * <p>
 * Copyright (C) 2015 by Aarhus University NanoHTTPD version 1.1, Copyright &copy; 2001,2005-2007 Jarno Elonen
 * (elonen@iki.fi, http://iki.fi/elonen/)
 * <p>
 */
public class NanoHTTPDJson
{
	public static String readContent(Properties header, BufferedReader in)
			throws IOException
	{
		long size = 0x7FFFFFFFFFFFFFFFl;
		String contentLength = header.getProperty("content-length").toString();
		if (contentLength != null)
		{
			try
			{
				size = Integer.parseInt(contentLength);
			} catch (NumberFormatException ex)
			{
			}
		}
		String postLine = "";

		if (contentLength != null)
		{
			char buf[] = new char[512];

			int read = in.read(buf);
			while (read >= 0 && size > 0 && !postLine.endsWith("\r\n"))
			{
				size -= read;
				postLine += String.valueOf(buf, 0, read);
				if (size > 0)
				{
					read = in.read(buf);
				}
			}
			postLine = postLine.trim();
		}
		return postLine;
	}

	//
	// API parts
	//

	private final RequestHandler handler;

	//
	// Socket & server code
	//

	/**
	 * Starts a HTTP server to given port.
	 * <p>
	 * Throws an IOException if the socket is already in use
	 */
	public NanoHTTPDJson(int port, RequestHandler handler) throws IOException
	{
		myTcpPort = port;
		this.handler = handler;

		serverSocket = new ServerSocket(myTcpPort);
		clientAccepterThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					while (true)
					{
						new HTTPSession(serverSocket.accept());
					}
				} catch (IOException ioe)
				{
				}
				try
				{
					serverSocket.close();
				} catch (IOException e)
				{
				}
			}
		});
		clientAccepterThread.setDaemon(true);
		clientAccepterThread.start();
	}

	public void stop()
	{
		try
		{
			clientAccepterThread.interrupt();
		} catch (Throwable t)
		{
		}

		try
		{
			serverSocket.close();
		} catch (Throwable t)
		{
		}
	}

	/**
	 * Starts as a standalone file server and waits for Enter.
	 */
	public static void main(String[] args)
	{
		System.out.println("NanoHTTPD 1.1 (C) 2001,2005-2007 Jarno Elonen\n"
				+ "(Command line options: [port] [--licence])\n");

		// Show licence if requested
		int lopt = -1;

		// Change port if requested
		int port = 80;
		if (args.length > 0 && lopt != 0)
		{
			port = Integer.parseInt(args[0]);
		}

		NanoHTTPDJson nh = null;
		try
		{
			nh = new NanoHTTPDJson(port, new RequestHandler()
			{

				@Override
				public Response doPut(Properties header, String url,
						BufferedReader dataReader)
				{
					String contentType = header.getProperty("content-type");

					if (contentType != null
							&& contentType.equals(Response.MIME_JSON))
					{
						String data;
						try
						{
							data = NanoHTTPDJson.readContent(header, dataReader);

							Response r = new Response(Response.HTTP_OK, "application/json", data);
							return r;
						} catch (IOException e)
						{
							e.printStackTrace();
							return new Response(Response.HTTP_NOTIMPLEMENTED, Response.MIME_PLAINTEXT, "Call type not implemented");
						}
					} else
					{
						return new Response(Response.HTTP_NOTIMPLEMENTED, Response.MIME_PLAINTEXT, "Call type not implemented");
					}
				}

				@Override
				public Response doPost(Properties header, String url,
						BufferedReader dataReader)
				{
					String contentType = header.getProperty("content-type");

					if (contentType != null
							&& contentType.equals(Response.MIME_JSON))
					{
						String data;
						try
						{
							data = NanoHTTPDJson.readContent(header, dataReader);

							Response r = new Response(Response.HTTP_OK, "application/json", data);
							return r;
						} catch (IOException e)
						{
							e.printStackTrace();
							return new Response(Response.HTTP_NOTIMPLEMENTED, Response.MIME_PLAINTEXT, "Call type not implemented");
						}
					} else
					{
						return new Response(Response.HTTP_NOTIMPLEMENTED, Response.MIME_PLAINTEXT, "Call type not implemented");
					}
				}

				@Override
				public Response doGet(Properties header, String url,
						BufferedReader dataReader)
				{
					return new Response(Response.HTTP_NOTIMPLEMENTED, Response.MIME_PLAINTEXT, "Call type not implemented");
				}
			});
		} catch (IOException ioe)
		{
			System.err.println("Couldn't start server:\n" + ioe);
			System.exit(-1);
		}

		System.out.println("Now serving files in port " + port + " from \""
				+ new File("").getAbsolutePath() + "\"");
		System.out.println("Hit Enter to stop.\n");

		try
		{
			System.in.read();
		} catch (Throwable t)
		{
		}
		nh.stop();
	}

	/**
	 * Handles one session, i.e. parses the HTTP request and returns the response.
	 */
	private class HTTPSession implements Runnable
	{
		public HTTPSession(Socket s)
		{
			mySocket = s;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}

		public void run()
		{
			try
			{
				InputStream is = mySocket.getInputStream();
				if (is == null)
				{
					return;
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(is));

				// Read the request line
				StringTokenizer st = new StringTokenizer(in.readLine());
				if (!st.hasMoreTokens())
				{
					sendError(Response.HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
				}

				String method = st.nextToken();

				if (!st.hasMoreTokens())
				{
					sendError(Response.HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
				}

				String url = st.nextToken();

				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names uppercase since they are
				// case insensitive and vary by client.
				Properties header = new Properties();
				if (st.hasMoreTokens())
				{
					String line = in.readLine();
					while (line.trim().length() > 0)
					{
						int p = line.indexOf(':');
						header.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
						line = in.readLine();
					}
				}

				processRequest(method, in, header, url);

				in.close();
			} catch (IOException ioe)
			{
				try
				{
					sendError(Response.HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: "
							+ ioe.getMessage());
				} catch (Throwable t)
				{
				}
			} catch (InterruptedException ie)
			{
				// Thrown by sendError, ignore and exit the thread.
			}
		}

		private void processRequest(String method, BufferedReader in,
				Properties header, String url) throws IOException,
				InterruptedException
		{
			// readContent(in, header);

			Response r = null;
			try
			{
				// If the method is POST, there may be parameters
				// in data section, too, read it:
				if (method.equalsIgnoreCase("GET"))
				{
					r = handler.doGet(header, url, in);
				} else if (method.equalsIgnoreCase("POST"))
				{
					r = handler.doPost(header, url, in);
				} else if (method.equalsIgnoreCase("PUT"))
				{
					r = handler.doPut(header, url, in);
				}
			} catch (Exception e)
			{
				sendError(Response.HTTP_INTERNALERROR, e.getMessage());
			}

			sendResponse(r.status, r.mimeType, r.header, r.data);
		}

		/**
		 * Returns an error message as a HTTP response and throws InterruptedException to stop furhter request
		 * processing.
		 */
		private void sendError(String status, String msg)
				throws InterruptedException
		{
			sendResponse(status, Response.MIME_PLAINTEXT, null, new ByteArrayInputStream(msg.getBytes()));
			throw new InterruptedException();
		}

		/**
		 * Sends given response to the socket.
		 */
		private void sendResponse(String status, String mime,
				Properties header, InputStream data)
		{
			try
			{
				if (status == null)
				{
					throw new Error("sendResponse(): Status can't be null.");
				}

				OutputStream out = mySocket.getOutputStream();
				PrintWriter pw = new PrintWriter(out);
				pw.print("HTTP/1.0 " + status + " \r\n");

				if (mime != null)
				{
					pw.print("Content-Type: " + mime + "\r\n");
				}

				if (header == null || header.getProperty("Date") == null)
				{
					pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
				}

				if (header != null)
				{
					Enumeration<?> e = header.keys();
					while (e.hasMoreElements())
					{
						String key = (String) e.nextElement();
						String value = header.getProperty(key);
						pw.print(key + ": " + value + "\r\n");
					}
				}

				pw.print("\r\n");
				pw.flush();

				if (data != null)
				{
					byte[] buff = new byte[2048];
					while (true)
					{
						int read = data.read(buff, 0, 2048);
						if (read <= 0)
						{
							break;
						}
						out.write(buff, 0, read);
					}
				}
				out.flush();
				out.close();
				if (data != null)
				{
					data.close();
				}
			} catch (IOException ioe)
			{
				// Couldn't write? No can do.
				try
				{
					mySocket.close();
				} catch (Throwable t)
				{
				}
			}
		}

		private Socket mySocket;
	};

	private int myTcpPort;

	/**
	 * GMT date formatter
	 */
	private static java.text.SimpleDateFormat gmtFrmt;

	private ServerSocket serverSocket;

	private Thread clientAccepterThread;
	static
	{
		gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

}