package parsetoolkit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

public class ParseToolKit {
    private static final Logger logger = LoggerFactory.getLogger(ParseToolKit.class);
    private static final int COLUMN_LENGTH = 20;
    private static final String COLUMN_LENGTH_STRING = "20s";
    private static final int DIVIDER_LENGTH = 64;
    private static final Map<String, String> TABLE_COLORPLAN = Map.of(
            "title", "\033[95m",      // Magenta
            "header", "\033[33m",     // Yellow
            "row", "\033[92m",        // Green
            "default", "\033[0m"      
    );
    /**
     * Searches for a specified key in a nested map or list structure and collects its values.
     *
     * @param items The map or list to search within.
     * @param key   The key to search for.
     * @param log   If true, logs the number of results found.
     * @return A list of values associated with the specified key.
     */
    public static List<Object> dictSearch(Object items, String key, boolean log) {
        List<Object> results = new ArrayList<>();
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(items);
        int resultNum = 0;

        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current instanceof Map<?, ?>) {
                Map<?, ?> currentMap = (Map<?, ?>) current;
                for (Map.Entry<?, ?> entry : currentMap.entrySet()) {
                    if (key.equals(entry.getKey())) {
                        results.add(entry.getValue());
                        resultNum++;
                    } else {
                        stack.push(entry.getValue());
                    }
                }
            } else if (current instanceof List<?>) {
                List<?> currentList = (List<?>) current;
                for (Object item : currentList) {
                    stack.push(item);
                }
            }
        }

        if (log) {
            logger.info("dict search completed, result_num: {}", resultNum);
        }

        return results;
    }

    /**
     * Compare two JSON-like structures (maps, lists) and output differences.
     *
     * @param item1 First JSON-like structure.
     * @param item2 Second JSON-like structure.
     * @param title Title for the difference table.
     * @param log   Whether to log the differences.
     * @return A map containing the differences.
     */
    public static Map<String, Pair<Object, Object>> spotDifference(Object item1, Object item2, String title, boolean log) {
        Map<String, Pair<Object, Object>> differences = new HashMap<>();

        compareObjects(item1, item2, "", differences);

        if (!differences.isEmpty()) {
            StringBuilder table = new StringBuilder();
            table.append(String.join("", Collections.nCopies(DIVIDER_LENGTH, "="))).append("\n");
            if (title != null && !title.isEmpty()) {
                table.append(TABLE_COLORPLAN.get("title")).append(title).append(TABLE_COLORPLAN.get("default")).append("\n");
            }
            table.append(String.format("%s%-" + COLUMN_LENGTH_STRING +  "%-" + COLUMN_LENGTH_STRING +  
            		"  %-" + COLUMN_LENGTH_STRING +  "%s\n",
                    TABLE_COLORPLAN.get("header"), "Field", "Item1", "Item2", TABLE_COLORPLAN.get("default")));
            table.append(String.join("", Collections.nCopies(DIVIDER_LENGTH, "-"))).append("\n");
            
            for (Map.Entry<String, Pair<Object, Object>> entry : differences.entrySet()) {
                String field = wrapText(entry.getKey(), COLUMN_LENGTH);
                String item1Value = wrapText(String.valueOf(entry.getValue().getFirst()), COLUMN_LENGTH);
                String item2Value = wrapText(String.valueOf(entry.getValue().getSecond()), COLUMN_LENGTH);
                
                String[] fieldLines = field.split("\n");
                String[] item1Lines = item1Value.split("\n");
                String[] item2Lines = item2Value.split("\n");
                int maxLines = Math.max(fieldLines.length, Math.max(item1Lines.length, item2Lines.length));
                
                for (int i = 0; i < maxLines; i++) {
                    String f = i < fieldLines.length ? fieldLines[i] : "";
                    String i1 = i < item1Lines.length ? item1Lines[i] : "";
                    String i2 = i < item2Lines.length ? item2Lines[i] : "";
                    table.append(String.format("%s%-" + COLUMN_LENGTH_STRING +  "  %-" + COLUMN_LENGTH_STRING +  
                    		"  %-" + COLUMN_LENGTH_STRING +  "%s\n",
                            TABLE_COLORPLAN.get("row"),
                            f,
                            i1,
                            i2,
                            TABLE_COLORPLAN.get("default")));
                }
                table.append(String.join("", Collections.nCopies(DIVIDER_LENGTH, "-"))).append("\n");
            }
            System.out.println(table.toString());

            if (log) {
                StringBuilder logTable = new StringBuilder();
                if (title != null && !title.isEmpty()) {
                    logTable.append(title).append("\n");
                }
                logTable.append(String.format("%-" + COLUMN_LENGTH_STRING +  "  %-" + 
                COLUMN_LENGTH_STRING +  "  %-" + COLUMN_LENGTH_STRING +  "\n", "Field", "Item1", "Item2"));
                logTable.append(String.join("", Collections.nCopies(DIVIDER_LENGTH, "-"))).append("\n");
                
                for (Map.Entry<String, Pair<Object, Object>> entry : differences.entrySet()) {
                    String field = wrapText(String.valueOf(entry.getKey()), COLUMN_LENGTH);
                    String item1Value = wrapText(String.valueOf(entry.getValue().getFirst()), COLUMN_LENGTH);
                    String item2Value = wrapText(String.valueOf(entry.getValue().getSecond()), COLUMN_LENGTH);
                    
                    String[] fieldLines = field.split("\n");
                    String[] item1Lines = item1Value.split("\n");
                    String[] item2Lines = item2Value.split("\n");
                    int maxLines = Math.max(fieldLines.length, Math.max(item1Lines.length, item2Lines.length));
                    
                    for (int i = 0; i < maxLines; i++) {
                        String f = i < fieldLines.length ? fieldLines[i] : "";
                        String i1 = i < item1Lines.length ? item1Lines[i] : "";
                        String i2 = i < item2Lines.length ? item2Lines[i] : "";
                        logTable.append(String.format("%-" + COLUMN_LENGTH_STRING +  "  %-" + 
                        COLUMN_LENGTH_STRING +  "  %-" + COLUMN_LENGTH_STRING +  "\n",
                                f,
                                i1,
                                i2));
                    }
                    logTable.append(String.join("", Collections.nCopies(DIVIDER_LENGTH, "-"))).append("\n");
                }
                logger.info("Differences found:\n{}", logTable.toString());
            }
        }

        return differences;
    }

    /**
     * Helper method to wrap text by inserting "\n" when exceeding the maximum length.
     *
     * @param text      The text to wrap.
     * @param maxLength The maximum length of each line.
     * @return The wrapped text with "\n" inserted.
     */
    private static String wrapText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        StringBuilder wrapped = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + maxLength, text.length());
            wrapped.append(text, index, end);
            if (end < text.length()) {
                wrapped.append("\n");
            }
            index += maxLength;
        }
        return wrapped.toString();
    }

    /**
     * Helper method to recursively compare two objects.
     */
    private static void compareObjects(Object obj1, Object obj2, String path, Map<String, Pair<Object, Object>> differences) {
        if (obj1 == null && obj2 == null) {
            return;
        }
        if (obj1 == null || obj2 == null) {
            differences.put(path, new Pair<>(obj1, obj2));
            return;
        }
        if (!obj1.getClass().equals(obj2.getClass())) {
            differences.put(path, new Pair<>(obj1, obj2));
            return;
        }
        if (obj1 instanceof Map<?, ?>) {
            Map<?, ?> map1 = (Map<?, ?>) obj1;
            Map<?, ?> map2 = (Map<?, ?>) obj2;
            Set<Object> keys = new HashSet<>();
            keys.addAll(map1.keySet());
            keys.addAll(map2.keySet());
            for (Object key : keys) {
                String newPath = path.isEmpty() ? key.toString() : path + "." + key;
                if (!map1.containsKey(key)) {
                    differences.put(newPath, new Pair<>(null, map2.get(key)));
                } else if (!map2.containsKey(key)) {
                    differences.put(newPath, new Pair<>(map1.get(key), null));
                } else {
                    compareObjects(map1.get(key), map2.get(key), newPath, differences);
                }
            }
        } else if (obj1 instanceof List<?>) {
            List<?> list1 = (List<?>) obj1;
            List<?> list2 = (List<?>) obj2;
            int minLen = Math.min(list1.size(), list2.size());
            for (int i = 0; i < minLen; i++) {
                String newPath = path + "[" + i + "]";
                compareObjects(list1.get(i), list2.get(i), newPath, differences);
            }
            if (list1.size() > list2.size()) {
                for (int i = minLen; i < list1.size(); i++) {
                    String newPath = path + "[" + i + "]";
                    differences.put(newPath, new Pair<>(list1.get(i), null));
                }
            } else if (list2.size() > list1.size()) {
                for (int i = minLen; i < list2.size(); i++) {
                    String newPath = path + "[" + i + "]";
                    differences.put(newPath, new Pair<>(null, list2.get(i)));
                }
            }
        } else {
            if (!obj1.equals(obj2)) {
                differences.put(path, new Pair<>(obj1, obj2));
            }
        }
    }

    /**
     * Prints a table of items in a list of maps.
     *
     * @param itemList The list of maps representing table rows.
     * @param title    The title of the table.
     * @param log      Whether to log the table.
     * @return The original list of maps.
     */
    public static List<Map<String, Object>> tablePrint(List<Map<String, Object>> itemList, String title, boolean log) {
        if (itemList == null || itemList.isEmpty()) {
            logger.warn("No items to display in table.");
            return itemList;
        }

        Set<String> allKeys = new HashSet<>();
        for (Map<String, Object> item : itemList) {
            allKeys.addAll(item.keySet());
        }
        List<String> sortedKeys = new ArrayList<>(allKeys);
        Collections.sort(sortedKeys);

        // Determine column widths based on the maximum content length
        Map<String, Integer> columnWidths = new HashMap<>();
        for (String key : sortedKeys) {
            int maxWidth = key.length();
            for (Map<String, Object> item : itemList) {
                Object value = item.getOrDefault(key, "");
                String[] lines = wrapText(value.toString(), COLUMN_LENGTH).split("\n");
                for (String line : lines) {
                    if (line.length() > maxWidth) {
                        maxWidth = Math.min(line.length(), COLUMN_LENGTH); 
                    }
                }
            }
            columnWidths.put(key, COLUMN_LENGTH);
        }

        StringBuilder table = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            table.append(TABLE_COLORPLAN.get("title")).append(title).append(TABLE_COLORPLAN.get("default")).append("\n");
        }

        // Header
        for (String key : sortedKeys) {
            table.append(String.format("%s%-" + columnWidths.get(key) + "s%s ", TABLE_COLORPLAN.get("header"), key, TABLE_COLORPLAN.get("default")));
        }
        table.append("\n");
        // Divider
        for (String key : sortedKeys) {
            table.append(String.join("", Collections.nCopies(columnWidths.get(key), "-")));
        }
        table.append("\n");

        // Rows
        for (Map<String, Object> item : itemList) {
            List<List<String>> wrappedCells = new ArrayList<>();
            int maxLines = 1;
            for (String key : sortedKeys) {
                Object value = item.getOrDefault(key, "");
                List<String> wrapped = Arrays.asList(wrapText(value.toString(), columnWidths.get(key)).split("\n"));
                wrappedCells.add(wrapped);
                if (wrapped.size() > maxLines) {
                    maxLines = wrapped.size();
                }
            }

            // Print each line of the wrapped cells
            for (int i = 0; i < maxLines; i++) {
                for (int j = 0; j < sortedKeys.size(); j++) {
                    String key = sortedKeys.get(j);
                    List<String> cellLines = wrappedCells.get(j);
                    String line = i < cellLines.size() ? cellLines.get(i) : "";
                    table.append(String.format("%s%-" + columnWidths.get(key) + "s%s ", TABLE_COLORPLAN.get("row"), line, TABLE_COLORPLAN.get("default")));
                }
                table.append("\n");
            }

            for (String key : sortedKeys) {
                table.append(String.join("", Collections.nCopies(columnWidths.get(key), "-")));
            }
            table.append("\n");
        }

        System.out.println(table.toString());

        // Log version without colors
        if (log) {
            StringBuilder logTable = new StringBuilder();
            if (title != null && !title.isEmpty()) {
                logTable.append(title).append("\n");
            }

            // Header
            for (String key : sortedKeys) {
                logTable.append(String.format("%-" + columnWidths.get(key) + "s ", key));
            }
            logTable.append("\n");

            for (String key : sortedKeys) {
                logTable.append(String.join("", Collections.nCopies(columnWidths.get(key), "-")));
            }
            logTable.append("\n");

            // Rows
            for (Map<String, Object> item : itemList) {

                List<List<String>> wrappedCells = new ArrayList<>();
                int maxLines = 1;
                for (String key : sortedKeys) {
                    Object value = item.getOrDefault(key, "");
                    List<String> wrapped = Arrays.asList(wrapText(value.toString(), columnWidths.get(key)).split("\n"));
                    wrappedCells.add(wrapped);
                    if (wrapped.size() > maxLines) {
                        maxLines = wrapped.size();
                    }
                }

                for (int i = 0; i < maxLines; i++) {
                    for (int j = 0; j < sortedKeys.size(); j++) {
                        String key = sortedKeys.get(j);
                        List<String> cellLines = wrappedCells.get(j);
                        String line = i < cellLines.size() ? cellLines.get(i) : "";
                        logTable.append(String.format("%-" + columnWidths.get(key) + "s ", line));
                    }
                    logTable.append("\n");
                }

                for (String key : sortedKeys) {
                    logTable.append(String.join("", Collections.nCopies(columnWidths.get(key), "-")));
                }
                logTable.append("\n");
            }

            logger.info("Info Table Output:\n{}", logTable.toString());
        }

        return itemList;
    }

    /**
     * Simple Pair class to hold two related objects.
     */
    public static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second){
            this.first = first;
            this.second = second;
        }

        public F getFirst(){
            return first;
        }

        public S getSecond(){
            return second;
        }
    }
}
