import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Load CSV data
df = pd.read_csv("network_results.csv")

# Filter throughput data
throughput_df = df[df.columns[-2:]].dropna()

# Latency Graphs (Individual Messages)
message_sizes = [8, 64, 256, 512]

for size in message_sizes:
    subset = df[df["Message Size"] == size]
    avg_latency = subset["Latency (µs)"].mean()
    
    plt.figure(figsize=(8, 4))
    plt.plot(subset["Message Number"], subset["Latency (µs)"], label=f"Latency {size}B", color='blue', alpha=0.7)
    plt.axhline(avg_latency, color='red', linestyle='--', label=f"Avg Latency: {avg_latency:.2f} µs")
    plt.xlabel("Message Number")
    plt.ylabel("Latency (µs)")
    plt.title(f"Latency for {size}B Messages")
    plt.legend()
    plt.grid(True)
    plt.show()

# Throughput Bar Graph
plt.figure(figsize=(8, 4))
sns.barplot(x=throughput_df["Message Size"], y=throughput_df["Throughput (Mbps)"], palette="Blues_r")
plt.xlabel("Message Size (Bytes)")
plt.ylabel("Throughput (Mbps)")
plt.title("Throughput vs Message Size")
plt.grid(axis="y")
plt.show()
