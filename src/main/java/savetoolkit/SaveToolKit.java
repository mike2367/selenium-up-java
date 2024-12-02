package savetoolkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import com.opencsv.CSVWriter;

public class SaveToolKit {

    public static void csvSave(String filename, Iterator<Map<String, String>> itemIterator, String encoding, int maxWorkers, boolean log) {
        ReentrantLock lock = new ReentrantLock();

        ExecutorService executor = Executors.newFixedThreadPool(maxWorkers);
        boolean first = true;

        try {
            while (itemIterator.hasNext()) {
                Map<String, String> item = itemIterator.next();
                boolean finalFirst = first;
                executor.submit(() -> {
                    writeSingleItem(filename, item, finalFirst, encoding, lock);
                });
                first = false;
            }
        } finally {
            executor.shutdown();
        }

        if (log) {
            System.out.println("Inserted records into " + filename);
        }
    }

    private static void writeSingleItem(String filename, Map<String, String> item, boolean writeHeaders, String encoding, ReentrantLock lock) {
        lock.lock();
        try {
            File file = new File(filename);
            
            if (!file.exists()) {
                boolean fileCreated = file.createNewFile(); 
                if (!fileCreated) {
                    System.err.println("Failed to create file: " + filename);
                    return;
                }
            }

            // Open the FileWriter in append mode
            try (FileWriter fileWriter = new FileWriter(file, true);
                 CSVWriter writer = new CSVWriter(fileWriter)) {
                if (writeHeaders) {
                    String[] header = item.keySet().toArray(new String[0]);
                    writer.writeNext(header);
                }
                String[] values = item.values().toArray(new String[0]);
                writer.writeNext(values);
                fileWriter.flush(); 
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error handling file operations: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}