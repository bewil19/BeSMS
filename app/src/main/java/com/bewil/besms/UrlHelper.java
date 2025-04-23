package com.bewil.besms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlHelper {

	public static String getPage(String targetUrl){
		HttpURLConnection connection = null;
		BufferedReader bufferedReader = null;

		try {
			URL url = new URL(targetUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(true);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-length", "0");
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);
			connection.connect();

			int status = connection.getResponseCode();
			if(status == 200 || status == 201){
				bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder stringBuilder = new StringBuilder();
				String line;
				while((line = bufferedReader.readLine()) != null){
					stringBuilder.append(line).append("\n");
				}
				return stringBuilder.toString();
			} else {
				return Integer.toString(status);
			}
		} catch (Exception exception){
			return null;
		} finally {
			try{
				if(bufferedReader != null){
					bufferedReader.close();
				}
			} catch (Exception ignored) {

			}

			if(connection != null){
				connection.disconnect();
			}
		}
	}
}
