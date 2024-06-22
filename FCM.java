import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FCM {

    private static final String SCOPES = "https://www.googleapis.com/auth/firebase.messaging";

    public static String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream("path/ServiceAccount.json"))
                .createScoped(SCOPES);

        googleCredentials.refreshIfExpired();
        AccessToken token = googleCredentials.getAccessToken();
        return token.getTokenValue();
    }

    private void sendNotification(Map<String,String> j) throws IOException {
        boolean sendStatus = false;
        HttpURLConnection conn = null;
        String strResponseText = "";

        try {

            String accessToken = getAccessToken();
            String sendUrl = "https://fcm.googleapis.com/v1/projects/<PROJECT_ID>/messages:send";
            URL url = new URL(sendUrl);

            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Content-Type", "application/json; UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);

            String payload = "{"
                    + "\"message\":{"
                    + "\"token\":\"" + j.get("token") + "\","
                    + "\"notification\":{"
                    + "\"title\":\"" + j.get("title") + "\","
                    + "\"body\":\"" + j.get("body") + "\""
                    + "}"
                    + "}"
                    + "}";

            conn.setConnectTimeout(2000);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            BufferedReader bufferedReader;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                sendStatus = true;
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = bufferedReader.readLine()) != null) {
                response.append(responseLine.trim());
            }
            bufferedReader.close();

            strResponseText = response.toString();
            System.out.println("Response: " + strResponseText);
            System.out.println("sendStatus : " + sendStatus);

        } catch(Exception e){
            System.out.println(strResponseText + "\nError sending 'POST' request to URL : " +
                    (conn != null ? conn.getResponseCode() : "") + "\n" + e);
            e.printStackTrace();
        } finally{
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static void main(String[] args) {
        try {
            Map<String,String> j = new HashMap<>();
            j.put("token", "<YOUR_CLIENT_FCM_TOKEN>");
			j.put("title", "Test Title");
            j.put("body", "Test body");
            new FCM().sendNotification(j);

        } catch (Exception e){
            System.out.println("e = " + e);
        }
    }

}