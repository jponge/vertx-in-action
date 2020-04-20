#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import argparse

parser = argparse.ArgumentParser(description="Plot some data from Hey csv output")
parser.add_argument("input", help="Data file in CSV format")
parser.add_argument("output", help="Output file name")
parser.add_argument("dpi", help="DPI resolution", type=int)
args = parser.parse_args()

data = pd.read_csv(args.input)

ok_values = data[data["status-code"] == 200]
ko_values = data[data["status-code"] != 200]

throughputs = data.round({"offset": 0}).groupby("offset").count()["status-code"]
for n in range(0, int(throughputs.index.max())):
  if throughputs.get(n) is None:
    throughputs[float(n)] = 0
throughputs = throughputs.sort_index()

dist = pd.DataFrame(columns=["percentile", "response-time"])
for q in [0.10, 0.50, 0.75, 0.80, 0.85, 0.90, 0.95, 0.98, 0.99, 0.999, 0.9999, 0.99999, 1.0]:
  dist = dist.append({"percentile": q * 100.0, "response-time": data["response-time"].quantile(q)}, ignore_index=True)

min_response = data["response-time"].min()
median_response = data["response-time"].median()
mean_response = data["response-time"].mean()
max_response = data["response-time"].max()
event_count = data["response-time"].count()
duration = data["offset"].max()
throughput = event_count / duration
throughput_min = throughputs.min()
throughput_max = throughputs.max()
throughput_median = throughputs.median()
n_ko = ko_values.shape[0]
n_ok = ok_values.shape[0]
if n_ok > n_ko:
  error_or_success_info = f"errors={round((n_ko / n_ok) * 100.0, 2)}%"
else:
  error_or_success_info = f"success={round((n_ok / n_ko) * 100.0, 2)}%"

fig, axs = plt.subplots(nrows=3, gridspec_kw=dict(height_ratios=[55, 25, 20]))

ok_values.plot(kind="scatter", x="offset", y="response-time", ax=axs[0], label="Success", color="tab:green")
ko_values.plot(kind="scatter", x="offset", y="response-time", ax=axs[0], label="Error", color="tab:red")

axs[0].set_xlabel("Time offset (s)")
axs[0].set_ylabel("Latency (s)")
axs[0].text(1, 1.1, f"min={min_response}, median={median_response}, max={max_response}, {event_count} events, {error_or_success_info}", fontsize=8, transform=axs[0].transAxes, horizontalalignment="right")

throughputs.plot(ax=axs[1], color="tab:blue")
axs[1].set_xlabel("Time offset (s)")
axs[1].set_ylabel("Requests / second")
axs[1].text(1, 1.2, f"min={throughput_min}, median={throughput_median}, max={throughput_max}", fontsize=8, transform=axs[1].transAxes, horizontalalignment="right")

dist.plot(kind="area", x="percentile", y="response-time", ax=axs[2], color="tab:purple", legend=False)

axs[2].set_xlabel("Percentiles")
axs[2].set_ylabel("Latency (s)")
axs[2].set_xlim(95.0, 100.0)

p95 = round(dist.loc[dist["percentile"] == 95.00]["response-time"].values[0], 3)
p99 = round(dist.loc[dist["percentile"] == 99.00]["response-time"].values[0], 3)
p999 = round(dist.loc[dist["percentile"] == 99.90]["response-time"].values[0], 3)
p9999 = round(dist.loc[dist["percentile"] == 99.99]["response-time"].values[0], 3)
p100 = round(dist.loc[dist["percentile"] == 100.0]["response-time"].values[0], 3)
axs[2].text(1, 1.3, f"p95={p95}, p99={p99}, p99.9={p999}, p9.999={p9999}, p100={p100}", fontsize=8, transform=axs[2].transAxes, horizontalalignment="right")

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
print(error_or_success_info)
print("duration =", duration, "(s)")
print("throughput = ", throughput, "(reqs/s) / min=", throughput_min, "max=", throughput_max)
