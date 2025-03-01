package TCP;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThroughputGraph {
    public static void main(String[] args) {
        String csvFile = "TCP_network_results.csv";  // Change to the correct file path
        Map<Integer, Double> throughputData = readThroughputCSV(csvFile);
        plotThroughputGraph(throughputData);
    }

    public static Map<Integer, Double> readThroughputCSV(String filePath) {
        Map<Integer, Double> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2 || values[0].equals("Message Size")) continue; // Skip invalid lines

                int messageSize = Integer.parseInt(values[0].trim());
                double throughput = Double.parseDouble(values[1].trim());

                data.put(messageSize, throughput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void plotThroughputGraph(Map<Integer, Double> throughputData) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<Integer, Double> entry : throughputData.entrySet()) {
            dataset.addValue(entry.getValue(), "Throughput (Mbps)", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Throughput vs Message Size", "Message Size (Bytes)", "Throughput (Mbps)",
                dataset
        );

        JFrame frame = new JFrame("Throughput Graph");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
