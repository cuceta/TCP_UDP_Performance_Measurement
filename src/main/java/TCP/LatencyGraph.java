package TCP;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class LatencyGraph {
    public static void main(String[] args) {
        String csvFile = "network_results.csv";  // Change to the correct file path
        Map<Integer, List<Long>> latencyData = readLatencyCSV(csvFile);

        for (Map.Entry<Integer, List<Long>> entry : latencyData.entrySet()) {
            int messageSize = entry.getKey();
            List<Long> latencies = entry.getValue();
            double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            plotLatencyGraph(latencies, "Latency for " + messageSize + " Byte Messages", avgLatency);
        }
    }

    public static Map<Integer, List<Long>> readLatencyCSV(String filePath) {
        Map<Integer, List<Long>> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 3 || values[0].equals("Message Size")) continue; // Skip invalid lines

                int messageSize = Integer.parseInt(values[0].trim());
                long latency = Long.parseLong(values[2].trim()); // Latency is the 3rd column

                data.putIfAbsent(messageSize, new ArrayList<>());
                data.get(messageSize).add(latency);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void plotLatencyGraph(List<Long> latencies, String title, double avgLatency) {
        XYSeries series = new XYSeries("Latency per message");
        for (int i = 0; i < latencies.size(); i++) {
            series.add(i + 1, latencies.get(i));  // X-axis: message number, Y-axis: latency
        }

        XYSeries avgSeries = new XYSeries("Average Latency");
        avgSeries.add(1, avgLatency);
        avgSeries.add(latencies.size(), avgLatency);  // Draw horizontal line for average

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(avgSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                title, "Message Number", "Latency (µs)", dataset,
                PlotOrientation.VERTICAL, true, true, false
        );

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
