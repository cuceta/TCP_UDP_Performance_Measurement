
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.*;
import java.util.*;

public class NetworkGraphGenerator {

//    private static String OUTPUT_DIR = "local-local";
//    private static String OUTPUT_DIR = "local-pi";
//    private static String OUTPUT_DIR = "pi-rho";
    private static String OUTPUT_DIR = "rho-moxie";


    public static void main(String[] args) {
        String tcpCsvFile = OUTPUT_DIR + "/TCP_network_results.csv";
        String udpCsvFile = OUTPUT_DIR + "/UDP_network_results.csv";

        runClientAndGenerateGraphs(tcpCsvFile, "TCP_");
        runClientAndGenerateGraphs(udpCsvFile, "UDP_");
    }

    private static void runClientAndGenerateGraphs(String csvFile, String prefix) {
        generateLatencyGraph(csvFile, prefix);
        generateThroughputGraph(csvFile, prefix);
    }

    public static void generateLatencyGraph(String filePath, String prefix) {
        Map<Integer, List<Integer>> latencyData = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 3) continue;
                try {
                    int messageSize = Integer.parseInt(values[0].trim());
                    int latency = Integer.parseInt(values[2].trim());

                    latencyData.putIfAbsent(messageSize, new ArrayList<>());
                    latencyData.get(messageSize).add(latency);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int[] sizes = {8, 64, 256, 512};
        for (int size : sizes) {
            if (latencyData.containsKey(size)) {
                saveLatencyChart(size, latencyData.get(size), prefix);
            }
        }
    }

    private static void saveLatencyChart(int messageSize, List<Integer> latencies, String prefix) {
        XYSeries series = new XYSeries("Latency");
        int sum = 0;
        for (int i = 0; i < latencies.size(); i++) {
            series.add(i + 1, latencies.get(i));
            sum += latencies.get(i);
        }
        double avgLatency = (double) sum / latencies.size();

        XYSeries avgSeries = new XYSeries("Average Latency");
        avgSeries.add(1, avgLatency);
        avgSeries.add(latencies.size(), avgLatency);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(avgSeries);

        JFreeChart latencyChart = ChartFactory.createXYLineChart(
                "Latency for " + messageSize + "B Messages",
                "Message Number",
                "Latency (µs)",
                dataset
        );

        try {
            ChartUtils.saveChartAsPNG(new File(OUTPUT_DIR, prefix + "latency_" + messageSize + "B.png"), latencyChart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateThroughputGraph(String filePath, String prefix) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Read and skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2) continue;
                try {
                    int messageSize = Integer.parseInt(values[0].trim());
                    if (messageSize == 8 || messageSize == 64) continue; // Exclude 8B and 64B sizes
                    double throughput = Double.parseDouble(values[1].trim());
                    dataset.addValue(throughput, "Throughput", messageSize + "B");
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        JFreeChart throughputChart = ChartFactory.createBarChart(
                "Throughput by Message Size",
                "Message Size (Bytes)",
                "Throughput (Mbps)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        try {
            ChartUtils.saveChartAsPNG(new File(OUTPUT_DIR, prefix + "throughput.png"), throughputChart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
