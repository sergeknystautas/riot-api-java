package net.rithms.riot.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Request {

    public static final int RETRY_ON_TIMEOUT = 5;
    public static final int RETRY_ON_RATE_LIMIT = 5;

    protected enum RequestMethod {
        DELETE, GET, POST, PUT
    }

    private static String execute(RequestMethod method, String requestURL,
            String key, String body) throws RiotApiException {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return executeRaw(method, requestURL, key, body);
            } catch (Exception e) {
                // System.out.println("*** " + e);
                if (e instanceof RiotApiException) {
                    if (((RiotApiException) e)
                            .getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
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
                msg += " " + rae.getMessage() + " - " + requestURL;
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

    private static String executeRaw(RequestMethod method, String requestURL,
            String key, String body) throws RiotApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(requestURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method.name());
            if (key != null) {
                connection.setRequestProperty("X-Riot-Token", key);
            }
            if (body != null) {
                connection.setRequestProperty("Content-Type",
                        "application/json");
                connection.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(
                        connection.getOutputStream());
                dos.writeBytes(body);
                dos.flush();
                dos.close();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                String retryAfterString = connection
                        .getHeaderField("Retry-After");
                String rateLimitType = connection
                        .getHeaderField("X-Rate-Limit-Type");
                if (retryAfterString != null) {
                    int retryAfter = Integer.parseInt(retryAfterString);
                    throw new RateLimitException(retryAfter, rateLimitType);
                } else {
                    throw new RateLimitException(0, rateLimitType);
                }
            } else if (responseCode < 200 || responseCode > 299) {
                throw new RiotApiException(responseCode);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append(System.lineSeparator());
            }
            br.close();
            connection.disconnect();
            return response.toString();
        } catch (IOException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null,
                    ex);
            throw new RiotApiException(RiotApiException.IOEXCEPTION);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // HTTP DELETE request
    protected static String sendDelete(String url, String key, String body)
            throws RiotApiException {
        return execute(RequestMethod.DELETE, url, key, body);
    }

    protected static String sendDelete(String url, String key)
            throws RiotApiException {
        return sendDelete(url, key, null);
    }

    protected static String sendDelete(String url) throws RiotApiException {
        return sendDelete(url, null);
    }

    // HTTP GET request
    protected static String sendGet(String url, String key, String body)
            throws RiotApiException {
        return execute(RequestMethod.GET, url, key, body);
    }

    protected static String sendGet(String url, String key)
            throws RiotApiException {
        return sendGet(url, key, null);
    }

    protected static String sendGet(String url) throws RiotApiException {
        return sendGet(url, null);
    }

    // HTTP POST request
    protected static String sendPost(String url, String key, String body)
            throws RiotApiException {
        return execute(RequestMethod.POST, url, key, body);
    }

    protected static String sendPost(String url, String key)
            throws RiotApiException {
        return sendPost(url, key, null);
    }

    protected static String sendPost(String url) throws RiotApiException {
        return sendPost(url, null);
    }

    // HTTP PUT request
    protected static String sendPut(String url, String key, String body)
            throws RiotApiException {
        return execute(RequestMethod.PUT, url, key, body);
    }

    protected static String sendPut(String url, String key)
            throws RiotApiException {
        return sendPut(url, key, null);
    }

    protected static String sendPut(String url) throws RiotApiException {
        return sendPut(url, null);
    }
}