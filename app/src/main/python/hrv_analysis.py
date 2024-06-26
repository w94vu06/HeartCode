import numpy as np
import heartpy as hp
import json

def hrv_analysis(data, sampling_rate):
    data_np = np.array(data)

    # 增強R峰信號
    data_np = hp.preprocessing.enhance_peaks(data_np, iterations=3)

    try:
        wd, m = hp.process(
            data_np,
            sample_rate=sampling_rate,
            calc_freq=True,
            clean_rr=True,
            clean_rr_method="z-score",
            reject_segmentwise=True,
        )
    except hp.exceptions.BadSignalWarning as e:
        return None, json.dumps({"error": str(e)}), json.dumps({"error": str(e)})

    filtered_r_peaks = wd["peaklist"]
    r_values = data_np[filtered_r_peaks]

    filtered_r_peaks = [int(i) for i in filtered_r_peaks]
    r_values = [float(i) for i in r_values]

    r_peaks_data = {
        "r_peaks": filtered_r_peaks,
    }

    r_values_data = {"r_values": r_values}

    r_peaks_json = json.dumps(r_peaks_data)
    r_values_json = json.dumps(r_values_data)

    return m, r_peaks_json, r_values_json
