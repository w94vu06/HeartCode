import numpy as np
import heartpy as hp
import json

def hrv_analysis(data, sampling_rate):
    # Convert the data to a NumPy array
    data_np = np.array(data)

    # Enhance the R-peak signal
    data_np = hp.preprocessing.enhance_peaks(data_np, iterations=2)

    # Perform HRV analysis to detect R-peak locations
    wd, m = hp.process(
        data_np,
        sample_rate=sampling_rate,
        windowsize=0.6,
        calc_freq=True,
        reject_segmentwise=True,
    )

    # Get the R-peaks and corresponding R-values
    filtered_r_peaks = wd["peaklist"]
    r_values = data_np[filtered_r_peaks]

    # Convert NumPy int64 to Python int
    filtered_r_peaks = [int(i) for i in filtered_r_peaks]
    r_values = [float(i) for i in r_values]

    # Combine the data into dictionaries and serialize to JSON
    r_peaks_data = {
        "r_peaks": filtered_r_peaks,  # No need to convert to list
    }

    r_values_data = {"r_values": r_values}  # Convert to list

    r_peaks_json = json.dumps(r_peaks_data)
    r_values_json = json.dumps(r_values_data)

    return m, r_peaks_json, r_values_json
