#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import argparse

parser = argparse.ArgumentParser(description="Plot some data from Hey csv output")
parser.add_argument("title", help="Plot title")
parser.add_argument("input", help="Data file in CSV format")
parser.add_argument("output", help="Output file name")
parser.add_argument("dpi", help="DPI resolution", type=int)
args = parser.parse_args()

data = pd.read_csv(args.input)

ok_values = data[data["status-code"] == 200]
ko_values = data[data["status-code"] != 200]

dist = pd.DataFrame(columns=["percentile", "response-time"])
for q in [0.1, 0.25, 0.5, 0.75, 0.8, 0.85, 0.9, 0.95, 0.98, 0.99, 0.999, 0.9999, 1.0]:
  dist = dist.append({"percentile": q * 100.0, "response-time": data["response-time"].quantile(q)}, ignore_index=True)

min_response = data["response-time"].min()
median_response = data["response-time"].median()
mean_response = data["response-time"].mean()
max_response = data["response-time"].max()
event_count = data["response-time"].count()
duration = data["offset"].max()
throughput = event_count / duration

fig, axs = plt.subplots(nrows=2, gridspec_kw=dict(height_ratios=[2, 1]))

ok_values.plot(kind="scatter", x="offset", y="response-time", ax=axs[0], label="HTTP 200", color="tab:blue")
ko_values.plot(kind="scatter", x="offset", y="response-time", ax=axs[0], label="HTTP 500", color="tab:red")

axs[0].set_xlabel("Time offset (s)")
axs[0].set_ylabel("Latency (s)")

dist.plot(kind="area", x="percentile", y="response-time", ax=axs[1], color="tab:purple", legend=False)

axs[1].set_xlabel("Percentiles")
axs[1].set_ylabel("Latency (s)")

fig.suptitle(f"{args.title} {round(duration, 2)}s run, {round(throughput, 2)} req/s")

plt.tight_layout(rect=[0, 0.03, 1, 0.95])

plt.savefig(f"{args.output}", dpi=args.dpi)

print("✨ Some stats...")
print()
print(dist)
print()
print("min =", min_response, "(s)")
print("median =", median_response, "(s)")
print("mean =", mean_response, "(s)")
print("max =", max_response, "(s)")
print()
print("count =", event_count, "events")
print("duration =", duration, "(s)")
print("throughput = ", throughput, "(reqs/s)")
