package org.intocps.orchestration.coe.httpserver;

import java.io.BufferedReader;
import java.util.Properties;

public interface RequestHandler
{
	Response doPost(Properties header, String url, BufferedReader dataReader);

	Response doPut(Properties header, String url, BufferedReader dataReader);

	Response doGet(Properties header, String url, BufferedReader dataReader);
}