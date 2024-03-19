package com.example.abgabem335jacketcheck;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WetterPage extends AppCompatActivity implements SensorEventListener {

    // API-URL, um Wetterdaten abzurufen
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/forecast?lat=44.34&lon=10.99&appid=83aefc1edd868497933110e51fe7fd1f";

    // TextView zur Anzeige der Wetterdaten und vom Namen
    private TextView apiTemperatureTextView;
    private TextView sensorTemperatureTextView;
    private String userName;

    // Sensor
    private SensorManager sensorManager;
    private Sensor temperatureSensor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wetter_page);

        // Den Namen des Benutzers von der ersten Seite
        userName = getIntent().getStringExtra("NAME");

        apiTemperatureTextView = findViewById(R.id.apiTemperatureTextView);
        sensorTemperatureTextView = findViewById(R.id.sensorTemperatureTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        // AsyncTask starten, um Wetterdaten abzurufen
        new FetchWeatherTask().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float currentTemperature = event.values[0];
            String temperatureMessage = "Aktuelle Sensor-Temperatur: " + String.format("%.2f", currentTemperature) + "°C";
            sensorTemperatureTextView.setText(temperatureMessage);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // AsyncTask-Klasse zum Abrufen von Wetterdaten im Hintergrund
    private class FetchWeatherTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Verbindung zur API herstellen -> Daten abrufen
                URL url = new URL(API_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray weatherArray = jsonObject.getJSONArray("list");
                    JSONObject weatherData = weatherArray.getJSONObject(0);
                    JSONObject main = weatherData.getJSONObject("main");
                    double temperature = main.getDouble("temp");
                    double temperatureCelsius = temperature - 273.15;

                    // Überprüfen der Temperatur
                    String jacketAdvice;
                    if (temperatureCelsius > 15) {
                        jacketAdvice = "Heute brauchen Sie keine Jacke.";
                    } else {
                        jacketAdvice = "Heute brauchen Sie eine Jacke.";
                        // Gerät kurz vibrieren lassen
                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        if (vibrator != null) {
                            vibrator.vibrate(500); // Vibrieren 500 Millisekunden
                        }
                    }

                    // Wetterinformationen im TextView anzeigen
                    String weatherMessage = userName + ", die Temperatur beträgt " + String.format("%.2f", temperatureCelsius) + "°C. " + jacketAdvice;
                    apiTemperatureTextView.setText(weatherMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                    apiTemperatureTextView.setText("Fehler beim Abrufen der Wetterdaten.");
                }
            } else {
                apiTemperatureTextView.setText("Fehler beim Abrufen der Wetterdaten. Bitte überprüfen Sie Ihre Internetverbindung.");
            }
        }
    }
}
