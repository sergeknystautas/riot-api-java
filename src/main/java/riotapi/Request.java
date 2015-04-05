package main.java.riotapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Request {

    public static final int RETRY_ON_TIMEOUT = 5;
    public static final int RETRY_ON_RATE_LIMIT = 5;

    /**
     * I want to retry the call many times because the Riot servers can suck, or
     * we have rate limits and we need to slow down the calls.
     * 
     * @param URL
     * @return
     * @throws RiotApiException
     */
    public static String execute(String URL) throws RiotApiException {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return executeRaw(URL);
            } catch (Exception e) {
                // System.out.println("*** " + e);
                if (e instanceof RiotApiException) {
                    if (((RiotApiException) e).getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
                        return null;
                    }
                } else if (e instanceof IOException) {
                    e = new RiotApiException(503);
                } else {
                    throw new RuntimeException(e);
                }
                RiotApiException rae = (RiotApiException) e;
                if (rae.getErrorCode() == RiotApiException.RATE_LIMITED) {
                    if (attempts >= RETRY_ON_RATE_LIMIT) {
                        throw rae;
                    }
                } else if (rae.getErrorCode() == RiotApiException.UNAVAILABLE) {
                    if (attempts >= RETRY_ON_TIMEOUT) {
                        throw rae;
                    }
                } else {
                    // This is some other error condition we are not handling
                    break;
                }
                String msg = "Retrying " + (attempts + 1) + " time";
                if (attempts > 0) {
                    msg += "s";
                }
                msg += " " + rae.getMessage() + " - " + URL;
                System.out.println(msg);
            }
            try {
                Thread.sleep(500 * (1 + attempts ^ attempts));
            } catch (InterruptedException e) {
                System.out.println("I do not care");
            }
        }
        return null;
    }

    public static String executeRaw(String URL) throws RiotApiException {
        HttpURLConnection connection = null;
        try {
            String requestURL = URL;
            URL url = new URL(requestURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RiotApiException(responseCode);
            }
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is,
                    "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            connection.disconnect();
            return response.toString();
        } catch (IOException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null,
                    ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
