import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;

// Retrieve weather data from API
public class WeatherApp {
    // Fetch weather app for given location
    public static JSONObject getWeatherData(String locationName){
        // get location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        // extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // Build api request url with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?"+
        "latitude="+latitude+"&longitude="+longitude+
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto";

        try{
            // call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check for response status
            if(conn.getResponseCode()!=200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // Store resulting data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            // Parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // Retrieve hourly data
            JSONObject hourly = (JSONObject) resultsJsonObj.get("hourly");
            // Get current hour data
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // Ge temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // Get weather code
            JSONArray weatherCode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weatherCode.get(index));

            // Get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // Get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            // Build the JSON data object that we are going to access in our frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            return weatherData;

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    // Retrieve geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName){
        // replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ","+");

        // Build APi url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name="
                +locationName+"&count=10&language=en&format=json";

        try{
            // call apu and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check response status
            if(conn.getResponseCode()!=200){
                System.out.println("Error: Could not connect to API");
                return null;
            }else{
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                // Read and store the resulting json data into our string builder
                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }
                scanner.close();

                conn.disconnect();

                // Parse the json string into a json obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // Get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set request method to get
            conn.setRequestMethod("GET");

            // Connect to our APi
            conn.connect();
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Couldn't make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        // Iterate through the time list and see which one matches our current time
        for(int i=0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }

        return 0;
    }

    public static String getCurrentTime(){
        // Get current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // format date to be readable by the api
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        // format and print the current date and time
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    // convert the weather code into something more readable
    public static String convertWeatherCode(long weatherCode){
        String weatherCondition = "";
        if (weatherCode == 0L){
            // Clear
            weatherCondition = "Clear";
        } else if (weatherCode <= 3L && weatherCode > 0L) {
            // cloudy
            weatherCondition = "Cloudy";
        } else if ((weatherCode >= 51L && weatherCode <= 67L)
                    || (weatherCode >= 80L && weatherCode <= 99L)) {
            // Rain
            weatherCondition = "Rain";
        } else if (weatherCode >= 71L && weatherCode <= 77L) {
            // Snow
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
