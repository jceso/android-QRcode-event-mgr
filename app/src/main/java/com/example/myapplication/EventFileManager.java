package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class EventFileManager {
    private static final String FILE_NAME = "saved_events.txt";

    // Save the set of event keys to a file
    public static void saveEvents(Context context, Set<String> eventKeys) {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            for (String key : eventKeys) {
                fos.write((key + "\n").getBytes()); // Write each event key on a new line
            }
        } catch (IOException e) {
            Log.e("EventFileManager", "Error saving events: " + e.getMessage());
        }
    }

    // Read the set of event keys from a file
    public static Set<String> loadEvents(Context context) {
        Set<String> eventKeys = new HashSet<>();
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                eventKeys.add(line.trim()); // Read each line and add it to the set
            }
        } catch (IOException e) {
            Log.e("EventFileManager", "Error reading events: " + e.getMessage());
        }
        return eventKeys;
    }
}
